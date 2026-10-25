package com.lyon.batterynotify.config

import android.os.Bundle
import com.lyon.batterynotify.BatteryConstants

data class BatteryConfig(
    val enabled: Boolean = BatteryConstants.DEFAULT_ENABLED,
    val monet: Boolean = BatteryConstants.DEFAULT_MONET,
    val displayMode: Int = BatteryConstants.DEFAULT_DISPLAY_MODE,
    val updateIntervalMs: Int = BatteryConstants.DEFAULT_UPDATE_INTERVAL_MS,
    val useDaemon: Boolean = BatteryConstants.DEFAULT_USE_DAEMON,
    val titleTemplate: String = BatteryConstants.DEFAULT_TITLE_TEMPLATE,
    val capsuleTemplate: String = BatteryConstants.DEFAULT_CAPSULE_TEMPLATE,
    val capsuleDisplayMode: Int = BatteryConstants.DEFAULT_CAPSULE_DISPLAY_MODE,
    val capsuleChargeDelay: Boolean = BatteryConstants.DEFAULT_CAPSULE_CHARGE_DELAY,
    val capsuleChargeDelaySec: Int = BatteryConstants.DEFAULT_CAPSULE_CHARGE_DELAY_SEC,
    val dualCell: Boolean = BatteryConstants.DEFAULT_DUAL_CELL,
    val showLockscreen: Boolean = BatteryConstants.DEFAULT_SHOW_LOCKSCREEN,
    val showStatusbarIcon: Boolean = BatteryConstants.DEFAULT_SHOW_STATUSBAR_ICON,
    val showLevel: Boolean = BatteryConstants.DEFAULT_SHOW_LEVEL,
    val showPower: Boolean = BatteryConstants.DEFAULT_SHOW_POWER,
    val showCurrent: Boolean = BatteryConstants.DEFAULT_SHOW_CURRENT,
    val showVoltage: Boolean = BatteryConstants.DEFAULT_SHOW_VOLTAGE,
    val showCharge: Boolean = BatteryConstants.DEFAULT_SHOW_CHARGE,
    val showCapacity: Boolean = BatteryConstants.DEFAULT_SHOW_CAPACITY,
    val showDesign: Boolean = BatteryConstants.DEFAULT_SHOW_DESIGN,
    val showEstimate: Boolean = BatteryConstants.DEFAULT_SHOW_ESTIMATE,
    val showTemperature: Boolean = BatteryConstants.DEFAULT_SHOW_TEMPERATURE,
    val showCycle: Boolean = BatteryConstants.DEFAULT_SHOW_CYCLE,
    val showHealth: Boolean = BatteryConstants.DEFAULT_SHOW_HEALTH,
    val showHealthPercent: Boolean = BatteryConstants.DEFAULT_SHOW_HEALTH_PERCENT,
    val showStatus: Boolean = BatteryConstants.DEFAULT_SHOW_STATUS,
) {
    fun toBundle(): Bundle = Bundle().apply {
        putBoolean(BatteryConstants.KEY_ENABLED, enabled)
        putBoolean(BatteryConstants.KEY_MONET, monet)
        putInt(BatteryConstants.KEY_DISPLAY_MODE, displayMode)
        putInt(BatteryConstants.KEY_UPDATE_INTERVAL_MS, updateIntervalMs)
        putBoolean(BatteryConstants.KEY_USE_DAEMON, useDaemon)
        putString(BatteryConstants.KEY_TITLE_TEMPLATE, titleTemplate)
        putString(BatteryConstants.KEY_CAPSULE_TEMPLATE, capsuleTemplate)
        putInt(BatteryConstants.KEY_CAPSULE_DISPLAY_MODE, capsuleDisplayMode)
        putBoolean(BatteryConstants.KEY_CAPSULE_CHARGE_DELAY, capsuleChargeDelay)
        putInt(BatteryConstants.KEY_CAPSULE_CHARGE_DELAY_SEC, capsuleChargeDelaySec)
        putBoolean(BatteryConstants.KEY_DUAL_CELL, dualCell)
        putBoolean(BatteryConstants.KEY_SHOW_LOCKSCREEN, showLockscreen)
        putBoolean(BatteryConstants.KEY_SHOW_STATUSBAR_ICON, showStatusbarIcon)
        putBoolean(BatteryConstants.KEY_SHOW_LEVEL, showLevel)
        putBoolean(BatteryConstants.KEY_SHOW_POWER, showPower)
        putBoolean(BatteryConstants.KEY_SHOW_CURRENT, showCurrent)
        putBoolean(BatteryConstants.KEY_SHOW_VOLTAGE, showVoltage)
        putBoolean(BatteryConstants.KEY_SHOW_CHARGE, showCharge)
        putBoolean(BatteryConstants.KEY_SHOW_CAPACITY, showCapacity)
        putBoolean(BatteryConstants.KEY_SHOW_DESIGN, showDesign)
        putBoolean(BatteryConstants.KEY_SHOW_ESTIMATE, showEstimate)
        putBoolean(BatteryConstants.KEY_SHOW_TEMPERATURE, showTemperature)
        putBoolean(BatteryConstants.KEY_SHOW_CYCLE, showCycle)
        putBoolean(BatteryConstants.KEY_SHOW_HEALTH, showHealth)
        putBoolean(BatteryConstants.KEY_SHOW_HEALTH_PERCENT, showHealthPercent)
        putBoolean(BatteryConstants.KEY_SHOW_STATUS, showStatus)
    }

    companion object {
        fun fromBundle(bundle: Bundle?): BatteryConfig {
            if (bundle == null) return BatteryConfig()
            return BatteryConfig(
                enabled = bundle.getBoolean(BatteryConstants.KEY_ENABLED, BatteryConstants.DEFAULT_ENABLED),
                monet = bundle.getBoolean(BatteryConstants.KEY_MONET, BatteryConstants.DEFAULT_MONET),
                displayMode = bundle.getInt(BatteryConstants.KEY_DISPLAY_MODE, BatteryConstants.DEFAULT_DISPLAY_MODE)
                    .coerceIn(BatteryConstants.DISPLAY_MODE_ALWAYS, BatteryConstants.DISPLAY_MODE_DISCHARGING),
                updateIntervalMs = bundle.getInt(BatteryConstants.KEY_UPDATE_INTERVAL_MS, BatteryConstants.DEFAULT_UPDATE_INTERVAL_MS)
                    .coerceIn(BatteryConstants.MIN_UPDATE_INTERVAL_MS, BatteryConstants.MAX_UPDATE_INTERVAL_MS),
                useDaemon = bundle.getBoolean(BatteryConstants.KEY_USE_DAEMON, BatteryConstants.DEFAULT_USE_DAEMON),
                titleTemplate = bundle.getString(BatteryConstants.KEY_TITLE_TEMPLATE) ?: BatteryConstants.DEFAULT_TITLE_TEMPLATE,
                capsuleTemplate = bundle.getString(BatteryConstants.KEY_CAPSULE_TEMPLATE) ?: BatteryConstants.DEFAULT_CAPSULE_TEMPLATE,
                capsuleDisplayMode = bundle.getInt(BatteryConstants.KEY_CAPSULE_DISPLAY_MODE, BatteryConstants.DEFAULT_CAPSULE_DISPLAY_MODE)
                    .coerceIn(BatteryConstants.DISPLAY_MODE_ALWAYS, BatteryConstants.DISPLAY_MODE_DISCHARGING),
                capsuleChargeDelay = bundle.getBoolean(BatteryConstants.KEY_CAPSULE_CHARGE_DELAY, BatteryConstants.DEFAULT_CAPSULE_CHARGE_DELAY),
                capsuleChargeDelaySec = bundle.getInt(BatteryConstants.KEY_CAPSULE_CHARGE_DELAY_SEC, BatteryConstants.DEFAULT_CAPSULE_CHARGE_DELAY_SEC).coerceIn(1, 60),
                dualCell = bundle.getBoolean(BatteryConstants.KEY_DUAL_CELL, BatteryConstants.DEFAULT_DUAL_CELL),
                showLockscreen = bundle.getBoolean(BatteryConstants.KEY_SHOW_LOCKSCREEN, BatteryConstants.DEFAULT_SHOW_LOCKSCREEN),
                showStatusbarIcon = bundle.getBoolean(BatteryConstants.KEY_SHOW_STATUSBAR_ICON, BatteryConstants.DEFAULT_SHOW_STATUSBAR_ICON),
                showLevel = bundle.getBoolean(BatteryConstants.KEY_SHOW_LEVEL, BatteryConstants.DEFAULT_SHOW_LEVEL),
                showPower = bundle.getBoolean(BatteryConstants.KEY_SHOW_POWER, BatteryConstants.DEFAULT_SHOW_POWER),
                showCurrent = bundle.getBoolean(BatteryConstants.KEY_SHOW_CURRENT, BatteryConstants.DEFAULT_SHOW_CURRENT),
                showVoltage = bundle.getBoolean(BatteryConstants.KEY_SHOW_VOLTAGE, BatteryConstants.DEFAULT_SHOW_VOLTAGE),
                showCharge = bundle.getBoolean(BatteryConstants.KEY_SHOW_CHARGE, BatteryConstants.DEFAULT_SHOW_CHARGE),
                showCapacity = bundle.getBoolean(BatteryConstants.KEY_SHOW_CAPACITY, BatteryConstants.DEFAULT_SHOW_CAPACITY),
                showDesign = bundle.getBoolean(BatteryConstants.KEY_SHOW_DESIGN, BatteryConstants.DEFAULT_SHOW_DESIGN),
                showEstimate = bundle.getBoolean(BatteryConstants.KEY_SHOW_ESTIMATE, BatteryConstants.DEFAULT_SHOW_ESTIMATE),
                showTemperature = bundle.getBoolean(BatteryConstants.KEY_SHOW_TEMPERATURE, BatteryConstants.DEFAULT_SHOW_TEMPERATURE),
                showCycle = bundle.getBoolean(BatteryConstants.KEY_SHOW_CYCLE, BatteryConstants.DEFAULT_SHOW_CYCLE),
                showHealth = bundle.getBoolean(BatteryConstants.KEY_SHOW_HEALTH, BatteryConstants.DEFAULT_SHOW_HEALTH),
                showHealthPercent = bundle.getBoolean(BatteryConstants.KEY_SHOW_HEALTH_PERCENT, BatteryConstants.DEFAULT_SHOW_HEALTH_PERCENT),
                showStatus = bundle.getBoolean(BatteryConstants.KEY_SHOW_STATUS, BatteryConstants.DEFAULT_SHOW_STATUS),
            )
        }
    }
}
