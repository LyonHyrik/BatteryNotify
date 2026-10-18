package com.lyon.batterynotify.core

/**
 * 电池数据快照。
 * SystemUI 侧 = 框架数据（ACTION_BATTERY_CHANGED + BatteryManager）+ 守护进程数据合并；
 * App 预览侧 = 仅框架数据（缺少的项显示 “—”）。
 */
data class BatteryInfo(
    val level: Int = -1,
    val scale: Int = 100,
    val status: Int = STATUS_UNKNOWN,
    val health: Int = HEALTH_UNKNOWN,
    val plugged: Int = 0,
    val voltageMv: Int = 0,
    val temperatureDeciC: Int = 0,
    val technology: String = "",
    val currentUa: Long? = null,
    val chargeCounterUah: Long? = null,
    // 守护进程数据（/sys/class/power_supply）
    val voltageNowUv: Long? = null,
    val currentNowUa: Long? = null,
    val powerNowUw: Long? = null,
    val chargeFullUah: Long? = null,
    val chargeFullDesignUah: Long? = null,
    // 厂商节点（oplus_chg）：实际满充容量 / 锁容设计容量 / 健康度
    val vendorFccUah: Long? = null,
    val vendorDesignUah: Long? = null,
    val vendorSoh: Int? = null,
    val cycleCount: Int? = null,
    val chargerName: String? = null,
    val chargeType: String? = null,
    val daemonAlive: Boolean = false,
) {
    val isCharging: Boolean
        get() = status == STATUS_CHARGING || (plugged != 0 && status != STATUS_DISCHARGING)

    companion object {
        // 与 android.os.BatteryManager 常量对齐（避免在纯模型层依赖 Android 类）
        const val STATUS_UNKNOWN = 1
        const val STATUS_CHARGING = 2
        const val STATUS_DISCHARGING = 3
        const val STATUS_NOT_CHARGING = 4
        const val STATUS_FULL = 5

        const val HEALTH_UNKNOWN = 1
        const val HEALTH_GOOD = 2
        const val HEALTH_OVERHEAT = 3
        const val HEALTH_DEAD = 4
        const val HEALTH_OVER_VOLTAGE = 5
        const val HEALTH_UNSPECIFIED_FAILURE = 6
        const val HEALTH_COLD = 7

        const val PLUGGED_NONE = 0
        const val PLUGGED_AC = 1
        const val PLUGGED_WIRELESS = 2
        const val PLUGGED_DOCK = 4
        const val PLUGGED_USB = 8
    }
}
