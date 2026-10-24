package com.lyon.batterynotify.systemui

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import com.lyon.batterynotify.BatteryConstants
import com.lyon.batterynotify.config.BatteryConfig

/**
 * SystemUI 侧配置桥：通过 ContentProvider.call 从模块 App 读取配置，
 * 并在模块 App 不可用时回退到本地缓存（与 RhythmicTouch 的 ConfigBridge 同构）。
 */
class ConfigBridge(context: Context) {
    private val resolver = context.contentResolver
    private val cachePrefs = context.getSharedPreferences("bn_systemui_cache", Context.MODE_PRIVATE)

    @Volatile
    var config: BatteryConfig = readCached()
        private set

    private var lastRefreshMs = 0L

    fun refresh(force: Boolean = false): BatteryConfig {
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastRefreshMs < 1000) return config
        lastRefreshMs = now
        val bundle: Bundle? = try {
            resolver.call(BatteryConstants.PROVIDER_URI, "get_config", null, null)
        } catch (t: Throwable) {
            null
        }
        if (bundle != null) {
            config = BatteryConfig.fromBundle(bundle)
            writeCache(config)
        }
        return config
    }

    private fun readCached(): BatteryConfig = BatteryConfig(
        enabled = cachePrefs.getBoolean(BatteryConstants.KEY_ENABLED, BatteryConstants.DEFAULT_ENABLED),
        monet = cachePrefs.getBoolean(BatteryConstants.KEY_MONET, BatteryConstants.DEFAULT_MONET),
        displayMode = cachePrefs.getInt(BatteryConstants.KEY_DISPLAY_MODE, BatteryConstants.DEFAULT_DISPLAY_MODE),
        updateIntervalMs = cachePrefs.getInt(BatteryConstants.KEY_UPDATE_INTERVAL_MS, BatteryConstants.DEFAULT_UPDATE_INTERVAL_MS),
        useDaemon = cachePrefs.getBoolean(BatteryConstants.KEY_USE_DAEMON, BatteryConstants.DEFAULT_USE_DAEMON),
        titleTemplate = cachePrefs.getString(BatteryConstants.KEY_TITLE_TEMPLATE, BatteryConstants.DEFAULT_TITLE_TEMPLATE)
            ?: BatteryConstants.DEFAULT_TITLE_TEMPLATE,
        showLockscreen = cachePrefs.getBoolean(BatteryConstants.KEY_SHOW_LOCKSCREEN, BatteryConstants.DEFAULT_SHOW_LOCKSCREEN),
        showStatusbarIcon = cachePrefs.getBoolean(BatteryConstants.KEY_SHOW_STATUSBAR_ICON, BatteryConstants.DEFAULT_SHOW_STATUSBAR_ICON),
        showLevel = cachePrefs.getBoolean(BatteryConstants.KEY_SHOW_LEVEL, BatteryConstants.DEFAULT_SHOW_LEVEL),
        showPower = cachePrefs.getBoolean(BatteryConstants.KEY_SHOW_POWER, BatteryConstants.DEFAULT_SHOW_POWER),
        showCurrent = cachePrefs.getBoolean(BatteryConstants.KEY_SHOW_CURRENT, BatteryConstants.DEFAULT_SHOW_CURRENT),
        showVoltage = cachePrefs.getBoolean(BatteryConstants.KEY_SHOW_VOLTAGE, BatteryConstants.DEFAULT_SHOW_VOLTAGE),
        showCharge = cachePrefs.getBoolean(BatteryConstants.KEY_SHOW_CHARGE, BatteryConstants.DEFAULT_SHOW_CHARGE),
        showCapacity = cachePrefs.getBoolean(BatteryConstants.KEY_SHOW_CAPACITY, BatteryConstants.DEFAULT_SHOW_CAPACITY),
        showDesign = cachePrefs.getBoolean(BatteryConstants.KEY_SHOW_DESIGN, BatteryConstants.DEFAULT_SHOW_DESIGN),
        showEstimate = cachePrefs.getBoolean(BatteryConstants.KEY_SHOW_ESTIMATE, BatteryConstants.DEFAULT_SHOW_ESTIMATE),
        showTemperature = cachePrefs.getBoolean(BatteryConstants.KEY_SHOW_TEMPERATURE, BatteryConstants.DEFAULT_SHOW_TEMPERATURE),
        showCycle = cachePrefs.getBoolean(BatteryConstants.KEY_SHOW_CYCLE, BatteryConstants.DEFAULT_SHOW_CYCLE),
        showHealth = cachePrefs.getBoolean(BatteryConstants.KEY_SHOW_HEALTH, BatteryConstants.DEFAULT_SHOW_HEALTH),
        showHealthPercent = cachePrefs.getBoolean(BatteryConstants.KEY_SHOW_HEALTH_PERCENT, BatteryConstants.DEFAULT_SHOW_HEALTH_PERCENT),
        showStatus = cachePrefs.getBoolean(BatteryConstants.KEY_SHOW_STATUS, BatteryConstants.DEFAULT_SHOW_STATUS),
    )

    fun writeCache(config: BatteryConfig) {
        try {
            cachePrefs.edit()
                .putBoolean(BatteryConstants.KEY_ENABLED, config.enabled)
                .putBoolean(BatteryConstants.KEY_MONET, config.monet)
                .putInt(BatteryConstants.KEY_DISPLAY_MODE, config.displayMode)
                .putInt(BatteryConstants.KEY_UPDATE_INTERVAL_MS, config.updateIntervalMs)
                .putBoolean(BatteryConstants.KEY_USE_DAEMON, config.useDaemon)
                .putString(BatteryConstants.KEY_TITLE_TEMPLATE, config.titleTemplate)
                .putBoolean(BatteryConstants.KEY_SHOW_LOCKSCREEN, config.showLockscreen)
                .putBoolean(BatteryConstants.KEY_SHOW_STATUSBAR_ICON, config.showStatusbarIcon)
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
        } catch (_: Throwable) {}
    }
}
