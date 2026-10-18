package com.lyon.batterynotify.systemui

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 电池精确数据读取器（无常驻进程）：
 *   1. 优先直接读取 /sys/class/power_supply（本机节点为全局可读，SystemUI 可直读）；
 *   2. 直读失败（如 SELinux 拒绝）时回退为 su -c 一次性读取；
 *   3. 结果缓存 1 秒，过期后异步刷新，调用线程立即返回旧数据。
 */
object RootReader {
    private const val TAG = "RootReader"
    private const val SU_BIN = "/system/bin/su"
    private const val CACHE_TTL_MS = 1000L
    private const val ROOT_RETRY_MS = 30_000L

    private val BATTERY_DIR = "/sys/class/power_supply/battery"
    private val BATTERY_FILES = listOf(
        "voltage_now", "current_now", "power_now", "charge_full",
        "charge_full_design", "cycle_count", "status", "health",
        "technology", "charge_type", "charge_counter",
    )

    /** oplus 厂商节点：实际满充容量 / 锁容设计容量 / 健康度（单位 mAh、%） */
    private val VENDOR_DIR = "/sys/devices/virtual/oplus_chg/battery"
    private val VENDOR_FILES = listOf("battery_fcc", "design_capacity", "battery_soh")

    private val SCRIPT = listOf(
        "B=/sys/class/power_supply/battery",
        "for f in voltage_now current_now power_now charge_full charge_full_design cycle_count status health technology charge_type charge_counter; do",
        "    v=\$(cat \$B/\$f 2>/dev/null) && [ -n \"\$v\" ] && echo \"\$f=\$v\"",
        "done",
        "V=/sys/devices/virtual/oplus_chg/battery",
        "for f in battery_fcc design_capacity battery_soh; do",
        "    v=\$(cat \$V/\$f 2>/dev/null) && [ -n \"\$v\" ] && echo \"\$f=\$v\"",
        "done",
        "for d in /sys/class/power_supply/*; do",
        "    o=\$(cat \$d/online 2>/dev/null)",
        "    if [ \"\$o\" = \"1\" ]; then",
        "        echo \"charger=\$(basename \$d)\"",
        "        break",
        "    fi",
        "done",
    ).joinToString("\n")

    @Volatile
    private var cache: Map<String, String> = emptyMap()

    @Volatile
    private var cacheTimeMs = 0L

    /** 精确数据是否可用（直读或 root 读取成功过） */
    @Volatile
    var available = false
        private set

    /** su 路径是否可用（仅在直读失败时会尝试） */
    @Volatile
    var rootAvailable = false
        private set

    private val executor: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "battery-root-reader").apply { isDaemon = true }
    }
    private val refreshing = AtomicBoolean(false)

    @Volatile
    private var lastRootCheckMs = 0L

    /** 异步刷新（若未在刷新中） */
    fun refreshAsync() {
        if (refreshing.compareAndSet(false, true)) {
            executor.execute {
                try {
                    doRead()
                } finally {
                    refreshing.set(false)
                }
            }
        }
    }

    /**
     * 读取快照（缓存 + 触发异步刷新）：
     *   - 缓存新鲜（<1s）直接返回；
     *   - 缓存过期或首次调用触发异步刷新并立即返回旧数据（可能为空）。
     */
    fun read(): Map<String, String> {
        val now = System.currentTimeMillis()
        if (now - cacheTimeMs > CACHE_TTL_MS) refreshAsync()
        return cache
    }

    private fun doRead() {
        try {
            // 1) 优先直读（节点通常全局可读，无需 spawn su）
            val direct = readDirect()
            if (direct.isNotEmpty()) {
                cache = direct
                cacheTimeMs = System.currentTimeMillis()
                if (!available) {
                    available = true
                    BatteryLog.x("direct sysfs read ok (${direct.size} keys)")
                }
                return
            }

            // 2) 直读失败，回退 su（root 检测带 30s 重试，避免首次失败后永久放弃）
            val now = System.currentTimeMillis()
            if (now - lastRootCheckMs > ROOT_RETRY_MS) {
                lastRootCheckMs = now
                rootAvailable = checkRoot()
                BatteryLog.x("su check: ${if (rootAvailable) "available" else "unavailable"}")
            }
            if (!rootAvailable) {
                available = false
                return
            }

            val process = Runtime.getRuntime().exec(arrayOf(SU_BIN, "-c", SCRIPT))
            val stdout = StringBuilder()
            val stderr = StringBuilder()
            val outThread = Thread {
                BufferedReader(InputStreamReader(process.inputStream)).use { r ->
                    var line: String?
                    while (r.readLine().also { line = it } != null) stdout.append(line).append('\n')
                }
            }
            val errThread = Thread {
                BufferedReader(InputStreamReader(process.errorStream)).use { r ->
                    var line: String?
                    while (r.readLine().also { line = it } != null) stderr.append(line).append('\n')
                }
            }
            outThread.start()
            errThread.start()
            val completed = process.waitFor(2, TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
                BatteryLog.x("su command timeout (2s)")
                available = false
                return
            }
            outThread.join(500)
            errThread.join(500)

            if (process.exitValue() != 0) {
                BatteryLog.x("su failed rc=${process.exitValue()}, stderr=${stderr.toString().trim()}")
                available = false
                return
            }

            val map = HashMap<String, String>(16)
            for (line in stdout.toString().lineSequence()) {
                val idx = line.indexOf('=')
                if (idx <= 0) continue
                map[line.substring(0, idx).trim()] = line.substring(idx + 1).trim()
            }
            if (map.isNotEmpty()) {
                cache = map
                cacheTimeMs = System.currentTimeMillis()
                if (!available) {
                    available = true
                    BatteryLog.x("su sysfs read ok (${map.size} keys)")
                }
            } else {
                available = false
            }
        } catch (t: Throwable) {
            BatteryLog.x("doRead failed: ${t.message}")
        }
    }

    /** 直接读取 sysfs 节点（不提权） */
    private fun readDirect(): Map<String, String> {
        val map = HashMap<String, String>(16)
        for (name in BATTERY_FILES) {
            val v = try {
                File(BATTERY_DIR, name).readText().trim()
            } catch (_: Throwable) {
                continue
            }
            if (v.isNotEmpty()) map[name] = v
        }
        for (name in VENDOR_FILES) {
            val v = try {
                File(VENDOR_DIR, name).readText().trim()
            } catch (_: Throwable) {
                continue
            }
            if (v.isNotEmpty()) map[name] = v
        }
        // 充电器在线检测
        try {
            File("/sys/class/power_supply").listFiles()?.forEach { d ->
                if (map.containsKey("charger")) return@forEach
                if (d.name == "battery") return@forEach
                val online = try {
                    File(d, "online").readText().trim()
                } catch (_: Throwable) {
                    ""
                }
                if (online == "1") map["charger"] = d.name
            }
        } catch (_: Throwable) {}
        return map
    }

    private fun checkRoot(): Boolean = try {
        val process = Runtime.getRuntime().exec(arrayOf(SU_BIN, "-c", "id"))
        val completed = process.waitFor(1, TimeUnit.SECONDS)
        if (!completed) {
            process.destroyForcibly()
            return false
        }
        process.exitValue() == 0
    } catch (_: Throwable) {
        false
    }
}
