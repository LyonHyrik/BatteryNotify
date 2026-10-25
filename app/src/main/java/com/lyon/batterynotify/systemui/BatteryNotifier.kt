package com.lyon.batterynotify.systemui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.lyon.batterynotify.BatteryConstants
import com.lyon.batterynotify.R
import com.lyon.batterynotify.config.BatteryConfig
import com.lyon.batterynotify.core.BatteryFormatter
import com.lyon.batterynotify.core.BatteryInfo

/**
 * 电池信息通知引擎（运行在被 hook 的 SystemUI 进程内）：
 *   - 监听 ACTION_BATTERY_CHANGED 并按配置节流，实时刷新通知；
 *   - 额外按 updateIntervalMs 周期性自刷新，保证 root 侧数据（功率等）也实时更新；
 *   - 合并 RootReader 从 /sys/class/power_supply 读取的精确数据；
 *   - 支持 显示模式（始终 / 仅充电时 / 仅放电时）。
 */
object BatteryNotifier {

    private const val TAG = "BatteryNotifier"

    private lateinit var context: Context
    private var configBridge: ConfigBridge? = null
    private var notificationManager: NotificationManager? = null

    @Volatile
    private var started = false

    @Volatile
    private var lastUpdateMs = 0L

    private var batteryReceiver: BroadcastReceiver? = null
    private var moduleReceiver: BroadcastReceiver? = null
    private val handler = Handler(Looper.getMainLooper())
    private var lastPluggedState: Int = -1
    private var plugChangedAtMs: Long = 0L

    private val tickRunnable = object : Runnable {
        override fun run() {
            try {
                if (started) {
                    val config = configBridge?.config ?: return
                    if (config.enabled) {
                        update(currentBatteryIntent(), config)
                    }
                    handler.postDelayed(this, config.updateIntervalMs.toLong())
                }
            } catch (t: Throwable) {
                BatteryLog.x("tick failed", t)
            }
        }
    }

    fun start(ctx: Context) {
        if (started) return
        started = true
        context = ctx.applicationContext
        BatteryLog.x("starting, process=${context.packageName}")

        configBridge = ConfigBridge(context)
        notificationManager = context.getSystemService(NotificationManager::class.java)
        createChannel()

        val config = configBridge!!.refresh(force = true)
        BatteryLog.x("initial config: enabled=${config.enabled}, mode=${config.displayMode}, interval=${config.updateIntervalMs}ms, root=${config.useDaemon}")

        if (config.useDaemon) {
            RootReader.refreshAsync()
        }

        registerBatteryReceiver()
        registerModuleReceiver()

        // 用粘性电池广播触发首次通知更新
        update(currentBatteryIntent(), config)
        handler.postDelayed(tickRunnable, config.updateIntervalMs.toLong())
    }

    /** 模块 App 配置变更后由 MainHook 调用 */
    fun onConfigChanged() {
        if (!started) return
        val config = configBridge?.refresh(force = true) ?: return
        BatteryLog.x("config refreshed: enabled=${config.enabled}, mode=${config.displayMode}, interval=${config.updateIntervalMs}ms, root=${config.useDaemon}")

        if (config.useDaemon) {
            RootReader.refreshAsync()
        }

        if (!config.enabled) {
            cancelNotification()
            return
        }
        update(currentBatteryIntent(), config)
    }

    fun onRestartDaemon() {
        if (!started) return
        val config = configBridge?.config ?: return
        if (config.useDaemon) {
            RootReader.refreshAsync()
            handler.postDelayed({ broadcastDaemonStatus() }, 2000)
        }
    }

    fun broadcastDaemonStatus() {
        try {
            val alive = RootReader.available
            val intent = Intent(BatteryConstants.ACTION_DAEMON_STATUS).apply {
                setPackage(BatteryConstants.MODULE_PACKAGE)
                putExtra(BatteryConstants.EXTRA_DAEMON_ALIVE, alive)
            }
            context.sendBroadcast(intent)
        } catch (t: Throwable) {
            BatteryLog.x("broadcastDaemonStatus failed", t)
        }
    }

    // ── 数据采集 ────────────────────────────────────────────────

    private fun collect(intent: Intent?, config: BatteryConfig): BatteryInfo {
        val i = intent ?: Intent(Intent.ACTION_BATTERY_CHANGED)
        val bm = context.getSystemService(BatteryManager::class.java)

        val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val status = i.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryInfo.STATUS_UNKNOWN)
        val health = i.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryInfo.HEALTH_UNKNOWN)
        val plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val voltageMv = i.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
        val tempDeciC = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        val technology = i.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: ""

        val currentUa = bm?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            ?.takeIf { it != Long.MIN_VALUE }
        val chargeCounterUah = bm?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            ?.takeIf { it != Long.MIN_VALUE && it > 0 }

        var voltageNowUv: Long? = null
        var currentNowUa: Long? = null
        var powerNowUw: Long? = null
        var chargeFullUah: Long? = null
        var chargeFullDesignUah: Long? = null
        var vendorFccUah: Long? = null
        var vendorDesignUah: Long? = null
        var vendorSoh: Int? = null
        var cycleCount: Int? = null
        var chargerName: String? = null
        var chargeType: String? = null
        var daemonAlive = false

        if (config.useDaemon) {
            val state = RootReader.read()
            daemonAlive = RootReader.available && state.isNotEmpty()
            if (daemonAlive) {
                // RootReader 返回 sysfs 原始值：
                //   voltage_now 单位 µV；current_now 可能是 mA 或 µA（由 BatteryFormatter 按量级归一）；
                //   power_now 可能是 µW 或 0（0 视为缺失，回退 V×I 估算）。
                voltageNowUv = state["voltage_now"]?.toLongOrNull()
                currentNowUa = state["current_now"]?.toLongOrNull()
                powerNowUw = state["power_now"]?.toLongOrNull()?.takeIf { it != 0L }
                chargeFullUah = state["charge_full"]?.toLongOrNull()?.takeIf { it > 0 }
                chargeFullDesignUah = state["charge_full_design"]?.toLongOrNull()?.takeIf { it > 0 }
                // oplus 厂商节点单位为 mAh，转成 µAh
                vendorFccUah = state["battery_fcc"]?.toLongOrNull()?.takeIf { it > 0 }?.let { it * 1000L }
                vendorDesignUah = state["design_capacity"]?.toLongOrNull()?.takeIf { it > 0 }?.let { it * 1000L }
                vendorSoh = state["battery_soh"]?.toIntOrNull()?.takeIf { it in 1..100 }
                cycleCount = state["cycle_count"]?.toIntOrNull()
                chargerName = state["charger"]
                chargeType = state["charge_type"]
            }
        }

        // RootReader 未提供电压时保持框架值即可
        return BatteryInfo(
            level = if (scale > 0 && level >= 0) level * 100 / scale else level,
            scale = scale,
            status = status,
            health = health,
            plugged = plugged,
            voltageMv = voltageMv,
            temperatureDeciC = tempDeciC,
            technology = technology,
            currentUa = currentUa,
            chargeCounterUah = chargeCounterUah,
            voltageNowUv = voltageNowUv,
            currentNowUa = currentNowUa,
            powerNowUw = powerNowUw,
            chargeFullUah = chargeFullUah,
            chargeFullDesignUah = chargeFullDesignUah,
            vendorFccUah = vendorFccUah,
            vendorDesignUah = vendorDesignUah,
            vendorSoh = vendorSoh,
            cycleCount = cycleCount,
            chargerName = chargerName,
            chargeType = chargeType,
            daemonAlive = daemonAlive,
        )
    }

    // ── 通知更新 ────────────────────────────────────────────────

    private fun update(intent: Intent?, config: BatteryConfig) {
        try {
            val now = SystemClock.elapsedRealtime()
            if (now - lastUpdateMs < config.updateIntervalMs) return
            lastUpdateMs = now

            if (config.useDaemon) {
                RootReader.refreshAsync()
            }

            val info = collect(intent, config)

            val visible = when (config.displayMode) {
                BatteryConstants.DISPLAY_MODE_CHARGING -> info.plugged != 0
                BatteryConstants.DISPLAY_MODE_DISCHARGING -> info.plugged == 0
                else -> true
            }
            if (!visible) {
                cancelNotification()
                return
            }

            val lines = BatteryFormatter.lines(info, config)
            if (lines.isEmpty()) {
                cancelNotification()
                return
            }

            // Android 16 Live Updates（ColorOS 16 流体云）
            val capsuleText = buildCapsuleText(info, config)
            var capsuleAllowed = when (config.capsuleDisplayMode) {
                BatteryConstants.DISPLAY_MODE_CHARGING -> info.plugged != 0
                BatteryConstants.DISPLAY_MODE_DISCHARGING -> info.plugged == 0
                else -> true
            }
            if (capsuleAllowed && config.capsuleChargeDelay && config.capsuleDisplayMode == BatteryConstants.DISPLAY_MODE_CHARGING && info.plugged != 0) {
                if (lastPluggedState == 0 && info.plugged != 0) {
                    plugChangedAtMs = SystemClock.elapsedRealtime()
                }
                if (lastPluggedState == -1) {
                    lastPluggedState = info.plugged
                    if (info.plugged != 0) {
                        plugChangedAtMs = SystemClock.elapsedRealtime()
                    }
                }
                val elapsed = (SystemClock.elapsedRealtime() - plugChangedAtMs) / 1000L
                if (elapsed < config.capsuleChargeDelaySec.toLong()) {
                    capsuleAllowed = false
                }
            }
            lastPluggedState = info.plugged
            try {
                supportsPromotedNotifications()
            } catch (_: Throwable) {
            }

            val builder = Notification.Builder(context, BatteryConstants.NOTIFICATION_CHANNEL_ID_V2)
                .setSmallIcon(resolveSmallIcon())
                .setContentTitle(BatteryFormatter.renderTitle(config.titleTemplate, info, config))
                .setContentText(BatteryFormatter.summary(lines))
                .setStyle(Notification.BigTextStyle().bigText(lines.joinToString("\n")))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_STATUS)
                .setVisibility(
                    if (config.showLockscreen) Notification.VISIBILITY_PUBLIC
                    else Notification.VISIBILITY_SECRET
                )

            if (capsuleAllowed) {
                try {
                    if (Build.VERSION.SDK_INT >= 36) {
                        builder.setShortCriticalText(capsuleText)
                    }
                } catch (t: Throwable) {
                    BatteryLog.x("setShortCriticalText failed", t)
                }
            }

            val notification = builder.build()

            try {
                val extras = notification.extras
                if (capsuleAllowed) {
                    extras.putBoolean("android.requestPromotedOngoing", true)
                    extras.putString("android.title.capsule", capsuleText)
                    extras.putString("android.capsule.title", capsuleText)
                }
                val isOplus = Build.MANUFACTURER.equals("OPPO", true) ||
                    Build.BRAND.equals("realme", true) ||
                    Build.MANUFACTURER.equals("OnePlus", true) ||
                    Build.BRAND.equals("OnePlus", true)
                if (isOplus) {
                    extras.putBoolean("oplus_keyguard_notification_transparent", true)
                    extras.putBoolean("oplus_keyguard_transparent", true)
                    extras.putInt("oplus_keyguard_bg_color", 0x00000000)
                    extras.putInt("oplus_keyguard_notification_bg_color", 0x00000000)
                    extras.putBoolean("oplus_keyguard_use_transparent_bg", true)
                }
            } catch (_: Throwable) {
            }

            notificationManager?.notify(BatteryConstants.NOTIFICATION_ID, notification)
        } catch (t: Throwable) {
            BatteryLog.x("update failed", t)
        }
    }

    private var cachedSmallIcon: Icon? = null

    private var promotedLogged = false

    private fun supportsPromotedNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < 36) return false
        return try {
            val ok = notificationManager?.canPostPromotedNotifications() == true
            if (!promotedLogged) {
                promotedLogged = true
                BatteryLog.x("canPostPromotedNotifications=$ok")
            }
            ok
        } catch (t: Throwable) {
            if (!promotedLogged) {
                promotedLogged = true
                BatteryLog.x("canPostPromotedNotifications failed", t)
            }
            false
        }
    }

    private fun buildCapsuleText(info: BatteryInfo, config: BatteryConfig): String {
        if (config.capsuleTemplate.isNotBlank()) {
            return BatteryFormatter.renderTitle(config.capsuleTemplate, info, config).replace("\n", " ")
        }
        val parts = mutableListOf<String>()
        if (config.showLevel) parts += "${info.level}%"
        val power = BatteryFormatter.powerW(info, config)
        if (power != null && Math.abs(power) > 0.05) {
            parts += if (info.plugged != 0) {
                String.format("%.1fW 充电", Math.abs(power))
            } else {
                String.format("%.1fW", Math.abs(power))
            }
        } else if (config.showStatus) {
            parts += when {
                info.status == BatteryInfo.STATUS_FULL -> "已充满"
                info.plugged != 0 && info.isCharging -> "充电中"
                info.plugged != 0 -> "已连接"
                else -> "放电中"
            }
        }
        return if (parts.isEmpty()) "${info.level}%" else parts.joinToString(" · ")
    }

    /**
     * 通知小图标：使用模块自带的"电池 + 闪电"矢量图。
     * 通知运行在 SystemUI 进程内，模块资源无法直接按 ID 引用，
     * 这里通过 PackageContext 加载并渲染成位图，再交给通知使用
     * （状态栏/通知栏只取小图标的 alpha 做单色着色，造型是关键）。
     * 失败时回退到框架内置的 stat_sys_battery。
     */
    private fun resolveSmallIcon(): Icon {
        cachedSmallIcon?.let { return it }
        val icon = try {
            val pkgCtx = context.createPackageContext(
                BatteryConstants.MODULE_PACKAGE,
                Context.CONTEXT_IGNORE_SECURITY,
            )
            val drawable = pkgCtx.getDrawable(R.drawable.ic_stat_battery)
                ?: error("drawable not found")
            val size = (24f * context.resources.displayMetrics.density + 0.5f)
                .toInt().coerceIn(48, 192)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, size, size)
            drawable.setTint(Color.WHITE)
            drawable.draw(Canvas(bitmap))
            Icon.createWithBitmap(bitmap)
        } catch (t: Throwable) {
            BatteryLog.x("render module icon failed, fallback to system", t)
            val id = context.resources.getIdentifier("stat_sys_battery", "drawable", "android")
            if (id != 0) Icon.createWithResource(context, id)
            else Icon.createWithResource(context, android.R.drawable.sym_def_app_icon)
        }
        cachedSmallIcon = icon
        return icon
    }

    private fun cancelNotification() {
        try {
            notificationManager?.cancel(BatteryConstants.NOTIFICATION_ID)
        } catch (_: Throwable) {}
    }

    // ── 注册与工具 ──────────────────────────────────────────────

    private fun currentBatteryIntent(): Intent? = try {
        @Suppress("DEPRECATION")
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    } catch (_: Throwable) {
        null
    }

    private fun createChannel() {
        try {
            val nm = notificationManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // ColorOS 对 SystemUI 包的既有渠道锁定重要性（mImportanceLockedDefaultApp=true），
                // 原地修改/importance 提升都会被 NMS 忽略，旧渠道还会被标成"不重要通知"
                // （mUnimportant=true），锁屏被过滤且系统设置里"锁屏显示"选项置灰。
                // 唯一可靠的规避方式：换一个全新的渠道 ID，用 IMPORTANCE_DEFAULT 创建，
                // 并显式声明锁屏可见；随后删除旧渠道避免残留。
                val channel = NotificationChannel(
                    BatteryConstants.NOTIFICATION_CHANNEL_ID_V2,
                    "电池信息",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    setSound(null, null)
                    enableVibration(false)
                    setShowBadge(false)
                    setLockscreenVisibility(Notification.VISIBILITY_PUBLIC)
                }
                nm.createNotificationChannel(channel)
                try {
                    nm.deleteNotificationChannel(BatteryConstants.NOTIFICATION_CHANNEL_ID)
                } catch (_: Throwable) {
                }
                val created = nm.getNotificationChannel(BatteryConstants.NOTIFICATION_CHANNEL_ID_V2)
                BatteryLog.x("channel v2 created: importance=${created?.importance}, lockscreenVis=${created?.lockscreenVisibility}")
            }
        } catch (t: Throwable) {
            BatteryLog.x("createChannel failed", t)
        }
    }

    private fun registerBatteryReceiver() {
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val recv = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    val config = configBridge?.refresh() ?: return
                    if (!config.enabled) {
                        cancelNotification()
                        return
                    }
                    update(intent, config)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(recv, filter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(recv, filter)
            }
            batteryReceiver = recv
        } catch (t: Throwable) {
            BatteryLog.x("registerBatteryReceiver failed", t)
        }
    }

    private fun registerModuleReceiver() {
        try {
            val filter = IntentFilter().apply {
                addAction(BatteryConstants.ACTION_REFRESH_CONFIG)
                addAction(BatteryConstants.ACTION_RESTART_DAEMON)
                addAction(BatteryConstants.ACTION_DAEMON_STATUS_REQUEST)
            }
            val recv = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    when (intent?.action) {
                        BatteryConstants.ACTION_REFRESH_CONFIG -> onConfigChanged()
                        BatteryConstants.ACTION_RESTART_DAEMON -> onRestartDaemon()
                        BatteryConstants.ACTION_DAEMON_STATUS_REQUEST -> broadcastDaemonStatus()
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(recv, filter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(recv, filter)
            }
            moduleReceiver = recv
        } catch (t: Throwable) {
            BatteryLog.x("registerModuleReceiver failed", t)
        }
    }
}
