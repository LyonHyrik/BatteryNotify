package com.lyon.batterynotify

import android.net.Uri

object BatteryConstants {
    const val MODULE_PACKAGE = "com.lyon.batterynotify"
    const val SYSTEMUI_PACKAGE = "com.android.systemui"
    const val PROVIDER_AUTHORITY = "com.lyon.batterynotify.provider"
    const val PREF_NAME = "battery_notify_config"

    // 配置键
    const val KEY_ENABLED = "enabled"
    const val KEY_MONET = "monet"
    const val KEY_DISPLAY_MODE = "display_mode"
    const val KEY_UPDATE_INTERVAL_MS = "update_interval_ms"
    const val KEY_USE_DAEMON = "use_daemon"
    const val KEY_TITLE_TEMPLATE = "title_template"
    const val KEY_CAPSULE_TEMPLATE = "capsule_template"
    const val KEY_CAPSULE_DISPLAY_MODE = "capsule_display_mode"
    const val KEY_CAPSULE_CHARGE_DELAY = "capsule_charge_delay"
    const val KEY_CAPSULE_CHARGE_DELAY_SEC = "capsule_charge_delay_sec"
    const val KEY_DUAL_CELL = "dual_cell"
    const val KEY_SHOW_LOCKSCREEN = "show_lockscreen"

    const val KEY_SHOW_LEVEL = "show_level"
    const val KEY_SHOW_POWER = "show_power"
    const val KEY_SHOW_CURRENT = "show_current"
    const val KEY_SHOW_VOLTAGE = "show_voltage"
    const val KEY_SHOW_CHARGE = "show_charge"
    const val KEY_SHOW_CAPACITY = "show_capacity"
    const val KEY_SHOW_DESIGN = "show_design"
    const val KEY_SHOW_ESTIMATE = "show_estimate"
    const val KEY_SHOW_TEMPERATURE = "show_temperature"
    const val KEY_SHOW_CYCLE = "show_cycle"
    const val KEY_SHOW_HEALTH = "show_health"
    const val KEY_SHOW_HEALTH_PERCENT = "show_health_percent"
    const val KEY_SHOW_STATUS = "show_status"
    const val KEY_SHOW_STATUSBAR_ICON = "show_statusbar_icon"

    // 显示模式
    const val DISPLAY_MODE_ALWAYS = 0
    const val DISPLAY_MODE_CHARGING = 1
    const val DISPLAY_MODE_DISCHARGING = 2

    // 默认值
    const val DEFAULT_ENABLED = true
    const val DEFAULT_MONET = true
    const val DEFAULT_DISPLAY_MODE = DISPLAY_MODE_ALWAYS
    const val DEFAULT_UPDATE_INTERVAL_MS = 1000
    const val DEFAULT_USE_DAEMON = true
    const val DEFAULT_TITLE_TEMPLATE = "电池信息 · {level}% · {status}"
    const val DEFAULT_CAPSULE_TEMPLATE = ""
    const val DEFAULT_CAPSULE_DISPLAY_MODE = DISPLAY_MODE_ALWAYS
    const val DEFAULT_CAPSULE_CHARGE_DELAY = false
    const val DEFAULT_CAPSULE_CHARGE_DELAY_SEC = 10
    const val DEFAULT_DUAL_CELL = false
    const val DEFAULT_SHOW_LOCKSCREEN = true

    const val DEFAULT_SHOW_LEVEL = true
    const val DEFAULT_SHOW_POWER = true
    const val DEFAULT_SHOW_CURRENT = true
    const val DEFAULT_SHOW_VOLTAGE = true
    const val DEFAULT_SHOW_CHARGE = true
    const val DEFAULT_SHOW_CAPACITY = true
    const val DEFAULT_SHOW_DESIGN = true
    const val DEFAULT_SHOW_ESTIMATE = true
    const val DEFAULT_SHOW_TEMPERATURE = true
    const val DEFAULT_SHOW_CYCLE = false
    const val DEFAULT_SHOW_HEALTH = true
    const val DEFAULT_SHOW_HEALTH_PERCENT = true
    const val DEFAULT_SHOW_STATUS = true
    const val DEFAULT_SHOW_STATUSBAR_ICON = true

    const val MIN_UPDATE_INTERVAL_MS = 500
    const val MAX_UPDATE_INTERVAL_MS = 30000

    // 广播
    const val ACTION_REFRESH_CONFIG = "com.lyon.batterynotify.ACTION_REFRESH_CONFIG"
    const val ACTION_RESTART_DAEMON = "com.lyon.batterynotify.ACTION_RESTART_DAEMON"
    const val ACTION_DAEMON_STATUS = "com.lyon.batterynotify.ACTION_DAEMON_STATUS"
    const val ACTION_DAEMON_STATUS_REQUEST = "com.lyon.batterynotify.ACTION_DAEMON_STATUS_REQUEST"
    const val EXTRA_DAEMON_ALIVE = "daemon_alive"

    // 通知
    const val NOTIFICATION_ID = 0xB477
    const val NOTIFICATION_CHANNEL_ID = "battery_notify_channel"
    // ColorOS 会锁定 SystemUI 旧渠道的重要性，换新 ID 重建才能使用 IMPORTANCE_DEFAULT
    const val NOTIFICATION_CHANNEL_ID_V2 = "battery_notify_channel_v2"

    // Provider
    const val KEY_MODULE_VERSION = "module_version_code"
    const val METHOD_SET_MODULE_VERSION = "set_module_version"
    const val METHOD_GET_MODULE_VERSION = "get_module_version"

    val PROVIDER_URI: Uri = Uri.parse("content://$PROVIDER_AUTHORITY")
}
