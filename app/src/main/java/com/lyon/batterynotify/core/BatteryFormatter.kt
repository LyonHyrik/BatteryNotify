package com.lyon.batterynotify.core

import com.lyon.batterynotify.BatteryConstants
import com.lyon.batterynotify.config.BatteryConfig
import java.util.Locale

/**
 * 电池信息文本格式化。
 * 注意：通知由 SystemUI 进程构建，无法访问模块 APK 的资源，
 * 因此这里使用硬编码文案（与 App 预览保持一致）。
 */
object BatteryFormatter {

    // ── 标题模板 ────────────────────────────────────────────────

    /**
     * 渲染自定义标题模板，支持变量：
     *   {level} {status} {power} {current} {voltage} {temp}
     *   {charge} {full} {design} {health} {cycle}
     *   {ttf}（预计充满） {tte}（预计可用） {charger}
     * 未知变量保持原样；模板为空时回退默认模板。
     */
    /** 已知变量名集合：有值时替换，无值时留空；未知变量保持原样便于调试 */
    private val TITLE_VARIABLES = setOf(
        "level", "status", "power", "current", "voltage", "temp",
        "charge", "full", "design", "health", "cycle", "ttf", "tte", "charger",
    )

    fun renderTitle(template: String, info: BatteryInfo, config: BatteryConfig): String {
        val tpl = template.trim().ifEmpty { BatteryConstants.DEFAULT_TITLE_TEMPLATE }
        val sb = StringBuilder(tpl.length + 32)
        var i = 0
        while (i < tpl.length) {
            val c = tpl[i]
            if (c == '{') {
                val end = tpl.indexOf('}', i + 1)
                if (end > i) {
                    val key = tpl.substring(i + 1, end)
                    val value = titleVariable(key, info, config)
                    when {
                        value != null -> sb.append(value)
                        key in TITLE_VARIABLES -> { /* 已知变量当前无值，留空 */ }
                        else -> sb.append(tpl, i, end + 1)
                    }
                    i = end + 1
                    continue
                }
            }
            sb.append(c)
            i++
        }
        return sb.toString()
    }

    private fun titleVariable(key: String, info: BatteryInfo, config: BatteryConfig): String? = when (key) {
        "level" -> if (info.level >= 0) "${info.level}" else null
        "status" -> statusShort(info)
        "power" -> powerW(info, config)?.let { format("%.2fW", it) }
        "current" -> effectiveCurrentUa(info, config)?.let { "${Math.abs(it) / 1000}mA" }
        "voltage" -> voltageMv(info)?.let { format("%.2fV", it / 1000.0) }
        "temp" -> temperatureC(info)?.let { format("%.1f°C", it) }
        "charge" -> info.chargeCounterUah?.let { "${it / 1000}mAh" }
        "full" -> effectiveFullUah(info)?.let { "${it / 1000}mAh" }
        "design" -> effectiveDesignUah(info)?.let { "${it / 1000}mAh" }
        "health" -> healthPercent(info)?.let { "$it%" }
        "cycle" -> info.cycleCount?.let { "${it}次" }
        "ttf" -> timeToFullText(info)
        "tte" -> timeToEmptyText(info, config)
        "charger" -> info.chargerName
        else -> null
    }

    /** 折叠态副标题：取前两个有效行 */
    fun summary(lines: List<String>): String =
        lines.take(2).joinToString("  ·  ")

    /** 生成全部启用项的文本行 */
    fun lines(info: BatteryInfo, config: BatteryConfig): List<String> {
        val out = ArrayList<String>(12)
        if (config.showLevel && info.level >= 0) {
            out += "电量 ${info.level}%"
        }
        if (config.showPower) {
            out += "功率 ${powerW(info, config)?.let { format("%.2fW", it) } ?: "—"}"
        }
        if (config.showCurrent) {
            val ua = effectiveCurrentUa(info, config)
            out += if (ua != null) {
                val sign = if (info.isCharging) "充电" else "放电"
                "电流 ${Math.abs(ua) / 1000}mA（$sign）"
            } else {
                "电流 —"
            }
        }
        if (config.showVoltage) {
            out += "电压 ${voltageMv(info)?.let { format("%.2fV", it / 1000.0) } ?: "—"}"
        }
        if (config.showCharge) {
            out += if (info.chargeCounterUah != null) {
                val pct = if (info.level >= 0) " · ${info.level}%" else ""
                "剩余容量 ${info.chargeCounterUah / 1000}mAh$pct"
            } else {
                "剩余容量 —"
            }
        }
        if (config.showCapacity) {
            out += "满充容量 ${effectiveFullUah(info)?.let { "${it / 1000}mAh" } ?: "—"}"
        }
        if (config.showDesign) {
            out += "设计容量 ${effectiveDesignUah(info)?.let { "${it / 1000}mAh" } ?: "—"}"
        }
        if (config.showEstimate) {
            val ttf = timeToFullText(info, config)
            val tte = timeToEmptyText(info, config)
            out += when {
                ttf != null -> "预计充满 $ttf"
                tte != null -> "预计可用 $tte"
                else -> "预计时间 —"
            }
        }
        if (config.showTemperature) {
            out += "温度 ${temperatureC(info)?.let { format("%.1f°C", it) } ?: "—"}"
        }
        if (config.showCycle) {
            out += "循环次数 ${info.cycleCount?.let { "${it}次" } ?: "—"}"
        }
        if (config.showHealth) {
            out += "健康状态 ${healthText(info.health)}"
        }
        if (config.showHealthPercent) {
            out += "电池健康度 ${healthPercent(info)?.let { "${it}%" } ?: "—"}"
        }
        if (config.showStatus) {
            out += "充电状态 ${statusLong(info)}"
        }
        return out
    }

    // ── 计算辅助 ────────────────────────────────────────────────

    fun effectiveVoltageUv(info: BatteryInfo): Long? =
        info.voltageNowUv ?: info.voltageMv.takeIf { it > 0 }?.let { it * 1000L }

    fun effectiveCurrentUa(info: BatteryInfo, config: BatteryConfig? = null): Long? {
        val ua = (info.currentNowUa ?: info.currentUa?.takeIf { it != 0L })?.let { normalizeUa(it) } ?: return null
        return if (config?.dualCell == true) ua * 2 else ua
    }

    /** 实际满充容量：优先厂商 FCC（oplus battery_fcc），回退 gauge charge_full */
    fun effectiveFullUah(info: BatteryInfo): Long? =
        info.vendorFccUah ?: info.chargeFullUah

    /** 设计容量：优先厂商锁容设计容量，回退 gauge charge_full_design */
    fun effectiveDesignUah(info: BatteryInfo): Long? =
        info.vendorDesignUah ?: info.chargeFullDesignUah

    /**
     * 电池健康度百分比：由实际满充容量 ÷ 设计容量 × 100 自行计算，
     * 不使用厂商 SOH 节点（厂商口径可能与锁容容量不一致）。
     */
    fun healthPercent(info: BatteryInfo): Int? {
        val full = effectiveFullUah(info) ?: return null
        val design = effectiveDesignUah(info)?.takeIf { it > 0 } ?: return null
        return (full * 100 / design).toInt().coerceIn(0, 100)
    }

    /**
     * 电流/功率单位自校正：不少机型（如本机 oplus）sysfs `current_now` 以 mA 上报，
     * `power_now` 以 mW 上报，数值绝对值远小于 µA/µW 量级。
     * 真实电池电流在 µA 量级下几乎不会低于 10 mA（=10000 µA），故按数量级判断：
     * 绝对值在 (0, 10000) 区间视为 mA/mW，换算成 µA/µW。
     */
    private const val UNIT_HEURISTIC_THRESHOLD = 10_000L

    fun normalizeUa(v: Long): Long =
        if (v != 0L && Math.abs(v) < UNIT_HEURISTIC_THRESHOLD) v * 1000L else v

    fun normalizeUw(v: Long): Long =
        if (v != 0L && Math.abs(v) < UNIT_HEURISTIC_THRESHOLD) v * 1000L else v

    /** 功率（µW）：优先 power_now，否则 电压 × 电流 估算 */
    fun powerUw(info: BatteryInfo, config: BatteryConfig? = null): Long? {
        val p = info.powerNowUw?.takeIf { it != 0L }?.let { Math.abs(normalizeUw(it)) }
        if (p != null) {
            return if (config?.dualCell == true) p * 2L else p
        }
        val v = effectiveVoltageUv(info) ?: return null
        val i = effectiveCurrentUa(info, config) ?: return null
        return v * Math.abs(i) / 1_000_000L
    }

    fun powerW(info: BatteryInfo, config: BatteryConfig? = null): Double? =
        powerUw(info, config)?.let { it / 1_000_000.0 }

    fun voltageMv(info: BatteryInfo): Int? = effectiveVoltageUv(info)?.let { (it / 1000L).toInt() }

    fun temperatureC(info: BatteryInfo): Double? =
        info.temperatureDeciC.takeIf { it != 0 }?.let { it / 10.0 }

    // ── 预计时间 ────────────────────────────────────────────────

    /**
     * 预计充满时间（分钟）：仅充电中可算。
     * (满充容量 − 剩余容量) ÷ 充电电流。
     * 注意：恒流/涓流阶段电流会变化，此为线性估算，末段会偏长。
     */
    fun timeToFullMinutes(info: BatteryInfo, config: BatteryConfig? = null): Long? {
        if (!info.isCharging) return null
        val remaining = info.chargeCounterUah ?: return null
        val full = effectiveFullUah(info) ?: return null
        val currentUa = effectiveCurrentUa(info, config)?.takeIf { it != 0L } ?: return null
        val missing = (full - remaining).coerceAtLeast(0L)
        // µAh ÷ µA = h → ×60 得分钟
        return missing * 60L / Math.abs(currentUa)
    }

    /**
     * 预计可用时间（分钟）：仅放电中可算。
     * 剩余容量 ÷ 放电电流。
     */
    fun timeToEmptyMinutes(info: BatteryInfo, config: BatteryConfig? = null): Long? {
        if (info.isCharging) return null
        val remaining = info.chargeCounterUah ?: return null
        val currentUa = effectiveCurrentUa(info, config)?.takeIf { it != 0L } ?: return null
        return remaining * 60L / Math.abs(currentUa)
    }

    fun timeToFullText(info: BatteryInfo, config: BatteryConfig? = null): String? = timeToFullMinutes(info, config)?.let { formatDuration(it) }

    fun timeToEmptyText(info: BatteryInfo, config: BatteryConfig? = null): String? = timeToEmptyMinutes(info, config)?.let { formatDuration(it) }

    /** 分钟 → “X小时Y分” / “Y分” */
    fun formatDuration(minutes: Long): String {
        val m = minutes.coerceAtLeast(0L)
        val h = m / 60
        val min = m % 60
        return when {
            h > 0 && min > 0 -> "${h}小时${min}分"
            h > 0 -> "${h}小时"
            else -> "${min}分"
        }
    }

    // ── 文案映射 ────────────────────────────────────────────────

    private fun statusShort(info: BatteryInfo): String = when {
        info.status == BatteryInfo.STATUS_FULL && info.plugged != 0 -> "已充满"
        info.plugged != 0 -> "充电中"
        else -> "放电中"
    }

    private fun statusLong(info: BatteryInfo): String {
        val base = when {
            info.status == BatteryInfo.STATUS_FULL && info.plugged != 0 -> "已充满"
            info.status == BatteryInfo.STATUS_CHARGING -> "充电中"
            info.status == BatteryInfo.STATUS_DISCHARGING -> "放电中"
            info.status == BatteryInfo.STATUS_NOT_CHARGING -> "放电中"
            else -> "放电中"
        }
        if (info.plugged == 0) return base
        val plug = when (info.plugged) {
            BatteryInfo.PLUGGED_AC -> "AC电源"
            BatteryInfo.PLUGGED_WIRELESS -> "无线充电"
            BatteryInfo.PLUGGED_DOCK -> "底座"
            BatteryInfo.PLUGGED_USB -> "USB"
            else -> "其他"
        }
        val type = info.chargeType?.takeIf { it.isNotBlank() && !it.equals("Normal", ignoreCase = true) }
        return if (type != null) "$base · $plug · $type" else "$base · $plug"
    }

    private fun healthText(health: Int): String = when (health) {
        BatteryInfo.HEALTH_GOOD -> "良好"
        BatteryInfo.HEALTH_OVERHEAT -> "过热"
        BatteryInfo.HEALTH_DEAD -> "损坏"
        BatteryInfo.HEALTH_OVER_VOLTAGE -> "电压过高"
        BatteryInfo.HEALTH_UNSPECIFIED_FAILURE -> "故障"
        BatteryInfo.HEALTH_COLD -> "过冷"
        else -> "未知"
    }

    private fun format(pattern: String, value: Double): String =
        String.format(Locale.getDefault(), pattern, value)
}
