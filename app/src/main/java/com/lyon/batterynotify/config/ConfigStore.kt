package com.lyon.batterynotify.config

import android.content.Context
import com.lyon.batterynotify.BatteryConstants

class ConfigStore(context: Context) {
    private val prefs = context.getSharedPreferences(BatteryConstants.PREF_NAME, Context.MODE_PRIVATE)

    fun read(): BatteryConfig = BatteryConfig(
        enabled = prefs.getBoolean(BatteryConstants.KEY_ENABLED, BatteryConstants.DEFAULT_ENABLED),
        monet = prefs.getBoolean(BatteryConstants.KEY_MONET, BatteryConstants.DEFAULT_MONET),
        displayMode = prefs.getInt(BatteryConstants.KEY_DISPLAY_MODE, BatteryConstants.DEFAULT_DISPLAY_MODE)
            .coerceIn(BatteryConstants.DISPLAY_MODE_ALWAYS, BatteryConstants.DISPLAY_MODE_DISCHARGING),
        updateIntervalMs = prefs.getInt(BatteryConstants.KEY_UPDATE_INTERVAL_MS, BatteryConstants.DEFAULT_UPDATE_INTERVAL_MS)
            .coerceIn(BatteryConstants.MIN_UPDATE_INTERVAL_MS, BatteryConstants.MAX_UPDATE_INTERVAL_MS),
        useDaemon = prefs.getBoolean(BatteryConstants.KEY_USE_DAEMON, BatteryConstants.DEFAULT_USE_DAEMON),
        titleTemplate = prefs.getString(BatteryConstants.KEY_TITLE_TEMPLATE, BatteryConstants.DEFAULT_TITLE_TEMPLATE)
            ?: BatteryConstants.DEFAULT_TITLE_TEMPLATE,
        showLockscreen = prefs.getBoolean(BatteryConstants.KEY_SHOW_LOCKSCREEN, BatteryConstants.DEFAULT_SHOW_LOCKSCREEN),
        showLevel = prefs.getBoolean(BatteryConstants.KEY_SHOW_LEVEL, BatteryConstants.DEFAULT_SHOW_LEVEL),
        showPower = prefs.getBoolean(BatteryConstants.KEY_SHOW_POWER, BatteryConstants.DEFAULT_SHOW_POWER),
        showCurrent = prefs.getBoolean(BatteryConstants.KEY_SHOW_CURRENT, BatteryConstants.DEFAULT_SHOW_CURRENT),
        showVoltage = prefs.getBoolean(BatteryConstants.KEY_SHOW_VOLTAGE, BatteryConstants.DEFAULT_SHOW_VOLTAGE),
        showCharge = prefs.getBoolean(BatteryConstants.KEY_SHOW_CHARGE, BatteryConstants.DEFAULT_SHOW_CHARGE),
        showCapacity = prefs.getBoolean(BatteryConstants.KEY_SHOW_CAPACITY, BatteryConstants.DEFAULT_SHOW_CAPACITY),
        showDesign = prefs.getBoolean(BatteryConstants.KEY_SHOW_DESIGN, BatteryConstants.DEFAULT_SHOW_DESIGN),
        showEstimate = prefs.getBoolean(BatteryConstants.KEY_SHOW_ESTIMATE, BatteryConstants.DEFAULT_SHOW_ESTIMATE),
        showTemperature = prefs.getBoolean(BatteryConstants.KEY_SHOW_TEMPERATURE, BatteryConstants.DEFAULT_SHOW_TEMPERATURE),
        showCycle = prefs.getBoolean(BatteryConstants.KEY_SHOW_CYCLE, BatteryConstants.DEFAULT_SHOW_CYCLE),
        showHealth = prefs.getBoolean(BatteryConstants.KEY_SHOW_HEALTH, BatteryConstants.DEFAULT_SHOW_HEALTH),
        showHealthPercent = prefs.getBoolean(BatteryConstants.KEY_SHOW_HEALTH_PERCENT, BatteryConstants.DEFAULT_SHOW_HEALTH_PERCENT),
        showStatus = prefs.getBoolean(BatteryConstants.KEY_SHOW_STATUS, BatteryConstants.DEFAULT_SHOW_STATUS),
    )

    fun write(config: BatteryConfig) {
        prefs.edit()
            .putBoolean(BatteryConstants.KEY_ENABLED, config.enabled)
            .putBoolean(BatteryConstants.KEY_MONET, config.monet)
            .putInt(BatteryConstants.KEY_DISPLAY_MODE, config.displayMode)
            .putInt(BatteryConstants.KEY_UPDATE_INTERVAL_MS, config.updateIntervalMs)
            .putBoolean(BatteryConstants.KEY_USE_DAEMON, config.useDaemon)
            .putString(BatteryConstants.KEY_TITLE_TEMPLATE, config.titleTemplate)
            .putBoolean(BatteryConstants.KEY_SHOW_LOCKSCREEN, config.showLockscreen)
            .putBoolean(BatteryConstants.KEY_SHOW_LEVEL, config.showLevel)
            .putBoolean(BatteryConstants.KEY_SHOW_POWER, config.showPower)
            .putBoolean(BatteryConstants.KEY_SHOW_CURRENT, config.showCurrent)
            .putBoolean(BatteryConstants.KEY_SHOW_VOLTAGE, config.showVoltage)
            .putBoolean(BatteryConstants.KEY_SHOW_CHARGE, config.showCharge)
            .putBoolean(BatteryConstants.KEY_SHOW_CAPACITY, config.showCapacity)
            .putBoolean(BatteryConstants.KEY_SHOW_DESIGN, config.showDesign)
            .putBoolean(BatteryConstants.KEY_SHOW_ESTIMATE, config.showEstimate)
            .putBoolean(BatteryConstants.KEY_SHOW_TEMPERATURE, config.showTemperature)
            .putBoolean(BatteryConstants.KEY_SHOW_CYCLE, config.showCycle)
            .putBoolean(BatteryConstants.KEY_SHOW_HEALTH, config.showHealth)
            .putBoolean(BatteryConstants.KEY_SHOW_HEALTH_PERCENT, config.showHealthPercent)
            .putBoolean(BatteryConstants.KEY_SHOW_STATUS, config.showStatus)
            .apply()
    }
}
