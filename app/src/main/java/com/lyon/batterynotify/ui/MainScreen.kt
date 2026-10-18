package com.lyon.batterynotify.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyon.batterynotify.BatteryConstants
import com.lyon.batterynotify.BuildConfig
import com.lyon.batterynotify.R
import com.lyon.batterynotify.config.BatteryConfig
import com.lyon.batterynotify.config.ConfigStore
import com.lyon.batterynotify.core.BatteryFormatter
import com.lyon.batterynotify.core.BatteryInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MainScreen(
    bottomBar: @Composable () -> Unit = {},
) {
    val context = LocalContext.current.applicationContext
    val store = remember { ConfigStore(context) }
    val config = remember { mutableStateOf(store.read()) }
    var hookVersionMismatch by remember { mutableStateOf(false) }
    var isRestarting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        hookVersionMismatch = checkHookVersionMismatch(context)
    }

    fun save(newConfig: BatteryConfig) {
        config.value = newConfig
        store.write(newConfig)
        context.sendBroadcast(Intent(BatteryConstants.ACTION_REFRESH_CONFIG))
    }

    fun restartSystemUI() {
        scope.launch {
            isRestarting = true
            try {
                withContext(Dispatchers.IO) {
                    val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "am crash com.android.systemui"))
                    process.waitFor()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                kotlinx.coroutines.delay(2000)
                isRestarting = false
            }
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(title = stringResource(R.string.screen_home))
        },
        bottomBar = bottomBar,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            if (hookVersionMismatch) {
                HookVersionBanner(
                    isRestarting = isRestarting,
                    onRestart = { restartSystemUI() },
                )
            }

            SuperSwitch(
                checked = config.value.enabled,
                onCheckedChange = { checked -> save(config.value.copy(enabled = checked)) },
                title = stringResource(R.string.setting_master),
                summary = stringResource(R.string.setting_master_desc),
            )

            LivePreviewCard(config = config.value)

            SmallTitle(stringResource(R.string.section_display_mode))

            DisplayModeSelector(
                mode = config.value.displayMode,
                onSelect = { mode -> save(config.value.copy(displayMode = mode)) },
            )

            SmallTitle(stringResource(R.string.section_display_items))

            ItemSwitch(
                checked = config.value.showLevel,
                onCheckedChange = { save(config.value.copy(showLevel = it)) },
                title = stringResource(R.string.item_level),
                summary = stringResource(R.string.item_level_desc),
            )
            ItemSwitch(
                checked = config.value.showPower,
                onCheckedChange = { save(config.value.copy(showPower = it)) },
                title = stringResource(R.string.item_power),
                summary = stringResource(R.string.item_power_desc),
            )
            ItemSwitch(
                checked = config.value.showCurrent,
                onCheckedChange = { save(config.value.copy(showCurrent = it)) },
                title = stringResource(R.string.item_current),
                summary = stringResource(R.string.item_current_desc),
            )
            ItemSwitch(
                checked = config.value.showVoltage,
                onCheckedChange = { save(config.value.copy(showVoltage = it)) },
                title = stringResource(R.string.item_voltage),
                summary = stringResource(R.string.item_voltage_desc),
            )
            ItemSwitch(
                checked = config.value.showCharge,
                onCheckedChange = { save(config.value.copy(showCharge = it)) },
                title = stringResource(R.string.item_charge),
                summary = stringResource(R.string.item_charge_desc),
            )
            ItemSwitch(
                checked = config.value.showCapacity,
                onCheckedChange = { save(config.value.copy(showCapacity = it)) },
                title = stringResource(R.string.item_capacity),
                summary = stringResource(R.string.item_capacity_desc),
            )
            ItemSwitch(
                checked = config.value.showDesign,
                onCheckedChange = { save(config.value.copy(showDesign = it)) },
                title = stringResource(R.string.item_design),
                summary = stringResource(R.string.item_design_desc),
            )
            ItemSwitch(
                checked = config.value.showEstimate,
                onCheckedChange = { save(config.value.copy(showEstimate = it)) },
                title = stringResource(R.string.item_time),
                summary = stringResource(R.string.item_time_desc),
            )
            ItemSwitch(
                checked = config.value.showTemperature,
                onCheckedChange = { save(config.value.copy(showTemperature = it)) },
                title = stringResource(R.string.item_temperature),
                summary = stringResource(R.string.item_temperature_desc),
            )
            ItemSwitch(
                checked = config.value.showCycle,
                onCheckedChange = { save(config.value.copy(showCycle = it)) },
                title = stringResource(R.string.item_cycle),
                summary = stringResource(R.string.item_cycle_desc),
            )
            ItemSwitch(
                checked = config.value.showHealth,
                onCheckedChange = { save(config.value.copy(showHealth = it)) },
                title = stringResource(R.string.item_health),
                summary = stringResource(R.string.item_health_desc),
            )
            ItemSwitch(
                checked = config.value.showHealthPercent,
                onCheckedChange = { save(config.value.copy(showHealthPercent = it)) },
                title = stringResource(R.string.item_health_percent),
                summary = stringResource(R.string.item_health_percent_desc),
            )
            ItemSwitch(
                checked = config.value.showStatus,
                onCheckedChange = { save(config.value.copy(showStatus = it)) },
                title = stringResource(R.string.item_status),
                summary = stringResource(R.string.item_status_desc),
            )

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ItemSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    summary: String,
) {
    SuperSwitch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        title = title,
        summary = summary,
    )
}

@Composable
private fun DisplayModeSelector(
    mode: Int,
    onSelect: (Int) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.section_display_mode),
                color = MiuixTheme.colorScheme.onSurfaceContainer,
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val options = listOf(
                    BatteryConstants.DISPLAY_MODE_ALWAYS to stringResource(R.string.mode_always),
                    BatteryConstants.DISPLAY_MODE_CHARGING to stringResource(R.string.mode_charging),
                    BatteryConstants.DISPLAY_MODE_DISCHARGING to stringResource(R.string.mode_discharging),
                )
                options.forEach { (value, label) ->
                    val selected = mode == value
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected) MiuixTheme.colorScheme.primary
                                else MiuixTheme.colorScheme.surfaceContainerHighest
                            )
                            .clickable { onSelect(value) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            color = if (selected) Color.White
                            else MiuixTheme.colorScheme.onSurfaceContainer,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LivePreviewCard(config: BatteryConfig) {
    val context = LocalContext.current.applicationContext
    var info by remember { mutableStateOf<BatteryInfo?>(null) }
    var daemonAlive by remember { mutableStateOf<Boolean?>(null) }

    DisposableEffect(Unit) {
        val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                info = intent?.toBatteryInfo(context)
            }
        }
        val daemonFilter = IntentFilter(BatteryConstants.ACTION_DAEMON_STATUS)
        val daemonReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                daemonAlive = intent?.getBooleanExtra(BatteryConstants.EXTRA_DAEMON_ALIVE, false)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(batteryReceiver, batteryFilter, Context.RECEIVER_EXPORTED)
            context.registerReceiver(daemonReceiver, daemonFilter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(batteryReceiver, batteryFilter)
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(daemonReceiver, daemonFilter)
        }
        @Suppress("DEPRECATION")
        info = context.registerReceiver(null, batteryFilter)?.toBatteryInfo(context)
        context.sendBroadcast(Intent(BatteryConstants.ACTION_DAEMON_STATUS_REQUEST))
        onDispose {
            try {
                context.unregisterReceiver(batteryReceiver)
                context.unregisterReceiver(daemonReceiver)
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(config.useDaemon) {
        if (!config.useDaemon) daemonAlive = null
    }

    val currentInfo = info
    val lines = currentInfo?.let { BatteryFormatter.lines(it, config) } ?: emptyList()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.section_preview),
                    color = MiuixTheme.colorScheme.onSurfaceContainer,
                )
                if (config.useDaemon) {
                    val aliveText = when (daemonAlive) {
                        true -> stringResource(R.string.daemon_running)
                        false -> stringResource(R.string.daemon_stopped)
                        null -> stringResource(R.string.daemon_querying)
                    }
                    val aliveColor = when (daemonAlive) {
                        true -> MiuixTheme.colorScheme.primary
                        false -> Color(0xFFE06C2C)
                        null -> MiuixTheme.colorScheme.onSurfaceContainerVariant
                    }
                    Text(
                        text = "${stringResource(R.string.preview_status_daemon)}: $aliveText",
                        color = aliveColor,
                        fontSize = 12.sp,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            if (lines.isEmpty()) {
                Text(
                    text = stringResource(R.string.preview_no_data),
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    fontSize = 13.sp,
                )
            } else {
                lines.forEach { line ->
                    Text(
                        text = line,
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.preview_hint),
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun HookVersionBanner(
    isRestarting: Boolean,
    onRestart: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = !isRestarting,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onRestart,
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.banner_version_mismatch),
                    color = Color(0xFFE06C2C),
                )
                Text(
                    text = if (isRestarting) {
                        stringResource(R.string.setting_restart_restarting)
                    } else {
                        "${stringResource(R.string.banner_restart_hint)} · ${stringResource(R.string.action_restart_systemui)}"
                    },
                    color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                )
            }
        }
    }
}

private fun Intent.toBatteryInfo(context: Context): BatteryInfo {
    val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = getIntExtra(BatteryManager.EXTRA_SCALE, 100)
    val currentUa = try {
        context.getSystemService(BatteryManager::class.java)
            ?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            ?.takeIf { it != Long.MIN_VALUE }
    } catch (_: Throwable) {
        null
    }
    val chargeCounter = try {
        context.getSystemService(BatteryManager::class.java)
            ?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            ?.takeIf { it != Long.MIN_VALUE && it > 0 }
    } catch (_: Throwable) {
        null
    }
    return BatteryInfo(
        level = if (scale > 0 && level >= 0) level * 100 / scale else level,
        scale = scale,
        status = getIntExtra(BatteryManager.EXTRA_STATUS, BatteryInfo.STATUS_UNKNOWN),
        health = getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryInfo.HEALTH_UNKNOWN),
        plugged = getIntExtra(BatteryManager.EXTRA_PLUGGED, 0),
        voltageMv = getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0),
        temperatureDeciC = getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0),
        technology = getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "",
        currentUa = currentUa,
        chargeCounterUah = chargeCounter,
    )
}

private fun checkHookVersionMismatch(context: Context): Boolean {
    return try {
        val result = context.contentResolver.call(
            BatteryConstants.PROVIDER_URI,
            BatteryConstants.METHOD_GET_MODULE_VERSION,
            null,
            null,
        )
        val hookVersion = result?.getInt(BatteryConstants.KEY_MODULE_VERSION, 0) ?: 0
        hookVersion != 0 && hookVersion != BuildConfig.VERSION_CODE
    } catch (_: Throwable) {
        false
    }
}
