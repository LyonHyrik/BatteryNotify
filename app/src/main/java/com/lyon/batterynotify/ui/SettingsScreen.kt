package com.lyon.batterynotify.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.extra.SuperSwitch
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SettingsScreen(
    store: ConfigStore,
    onMonetChange: (Boolean) -> Unit,
    bottomBar: @Composable () -> Unit = {},
) {
    val context = LocalContext.current.applicationContext
    val config = remember { mutableStateOf(store.read()) }
    var isRestarting by remember { mutableStateOf(false) }
    var daemonAlive by remember { mutableStateOf<Boolean?>(null) }
    val scope = rememberCoroutineScope()
    var showAbout by remember { mutableStateOf(false) }

    if (showAbout) {
        AboutScreen(
            onBack = { showAbout = false },
            monetEnabled = config.value.monet,
        )
        return
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

    DisposableEffect(Unit) {
        val filter = IntentFilter(BatteryConstants.ACTION_DAEMON_STATUS)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                daemonAlive = intent?.getBooleanExtra(BatteryConstants.EXTRA_DAEMON_ALIVE, false)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }
        context.sendBroadcast(Intent(BatteryConstants.ACTION_DAEMON_STATUS_REQUEST))
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(title = stringResource(R.string.screen_settings))
        },
        bottomBar = bottomBar,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // ── 外观 ────────────────────────────────────────────
            SmallTitle(stringResource(R.string.section_appearance))

            SuperSwitch(
                checked = config.value.monet,
                onCheckedChange = { checked ->
                    config.value = config.value.copy(monet = checked)
                    store.write(config.value)
                    onMonetChange(checked)
                },
                title = stringResource(R.string.setting_monet),
                summary = stringResource(R.string.setting_monet_desc),
            )

            // ── 通知 ────────────────────────────────────────────
            SmallTitle(stringResource(R.string.section_notification))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.setting_update_interval),
                            color = MiuixTheme.colorScheme.onSurfaceContainer,
                        )
                        Text(
                            text = String.format("%.1fs", config.value.updateIntervalMs / 1000.0),
                            color = MiuixTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        text = stringResource(R.string.setting_update_interval_desc),
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                        fontSize = 12.sp,
                    )
                    Slider(
                        value = config.value.updateIntervalMs.toFloat(),
                        onValueChange = { value ->
                            config.value = config.value.copy(updateIntervalMs = value.toInt())
                        },
                        valueRange = BatteryConstants.MIN_UPDATE_INTERVAL_MS.toFloat()..BatteryConstants.MAX_UPDATE_INTERVAL_MS.toFloat(),
                        onValueChangeFinished = { save(config.value) },
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = stringResource(R.string.setting_title_template),
                        color = MiuixTheme.colorScheme.onSurfaceContainer,
                    )
                    Text(
                        text = stringResource(R.string.setting_title_template_desc),
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    TextField(
                        value = config.value.titleTemplate,
                        onValueChange = { template ->
                            save(config.value.copy(titleTemplate = template))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = true,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.setting_title_template_hint),
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                        fontSize = 11.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.setting_title_template_preview) +
                            BatteryFormatter.renderTitle(config.value.titleTemplate, previewInfo(context), config.value),
                        color = MiuixTheme.colorScheme.primary,
                        fontSize = 12.sp,
                    )
                }
            }

            SuperSwitch(
                checked = config.value.showLockscreen,
                onCheckedChange = { checked -> save(config.value.copy(showLockscreen = checked)) },
                title = stringResource(R.string.setting_lockscreen),
                summary = stringResource(R.string.setting_lockscreen_desc),
            )

            Spacer(Modifier.height(8.dp))

            SuperSwitch(
                checked = !config.value.showStatusbarIcon,
                onCheckedChange = { checked -> save(config.value.copy(showStatusbarIcon = !checked)) },
                title = stringResource(R.string.setting_statusbar_icon),
                summary = stringResource(R.string.setting_statusbar_icon_desc),
            )

            // ── 守护进程 ────────────────────────────────────────
            SmallTitle(stringResource(R.string.section_daemon))

            SuperSwitch(
                checked = config.value.useDaemon,
                onCheckedChange = { checked -> save(config.value.copy(useDaemon = checked)) },
                title = stringResource(R.string.setting_use_daemon),
                summary = stringResource(R.string.setting_use_daemon_desc),
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            enabled = config.value.useDaemon,
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) {
                            context.sendBroadcast(Intent(BatteryConstants.ACTION_RESTART_DAEMON))
                            daemonAlive = null
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.daemon_status_title),
                            color = MiuixTheme.colorScheme.onSurfaceContainer,
                        )
                        val statusText = when {
                            !config.value.useDaemon -> stringResource(R.string.daemon_status_disabled)
                            daemonAlive == true -> stringResource(R.string.daemon_status_running)
                            daemonAlive == false -> stringResource(R.string.daemon_status_stopped)
                            else -> stringResource(R.string.daemon_querying)
                        }
                        Text(
                            text = "$statusText · ${stringResource(R.string.daemon_action_restart_desc)}",
                            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                        )
                    }
                }
            }

            // ── 系统 ────────────────────────────────────────────
            SmallTitle(stringResource(R.string.section_system))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isRestarting) { restartSystemUI() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.setting_restart_systemui),
                            color = MiuixTheme.colorScheme.onSurfaceContainer,
                        )
                        Text(
                            text = if (isRestarting) {
                                stringResource(R.string.setting_restart_restarting)
                            } else {
                                stringResource(R.string.setting_restart_desc)
                            },
                            color = if (isRestarting) MiuixTheme.colorScheme.primary
                            else MiuixTheme.colorScheme.onSurfaceContainerVariant,
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { showAbout = true },
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Text(
                        text = stringResource(R.string.setting_about),
                        color = MiuixTheme.colorScheme.onSurfaceContainer,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.app_description),
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.label_version, BuildConfig.VERSION_NAME),
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                        fontSize = 12.sp,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

/** 标题模板预览用：从粘性电池广播构造当前电池快照 */
private fun previewInfo(context: android.content.Context): BatteryInfo {
    return try {
        @Suppress("DEPRECATION")
        val i = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        if (i == null) BatteryInfo() else {
            val level = i.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
            val scale = i.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, 100)
            BatteryInfo(
                level = if (scale > 0 && level >= 0) level * 100 / scale else level,
                scale = scale,
                status = i.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, BatteryInfo.STATUS_UNKNOWN),
                health = i.getIntExtra(android.os.BatteryManager.EXTRA_HEALTH, BatteryInfo.HEALTH_UNKNOWN),
                plugged = i.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, 0),
                voltageMv = i.getIntExtra(android.os.BatteryManager.EXTRA_VOLTAGE, 0),
                temperatureDeciC = i.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, 0),
                technology = i.getStringExtra(android.os.BatteryManager.EXTRA_TECHNOLOGY) ?: "",
                currentUa = try {
                    context.getSystemService(android.os.BatteryManager::class.java)
                        ?.getLongProperty(android.os.BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                        ?.takeIf { it != Long.MIN_VALUE }
                } catch (_: Throwable) { null },
                chargeCounterUah = try {
                    context.getSystemService(android.os.BatteryManager::class.java)
                        ?.getLongProperty(android.os.BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
                        ?.takeIf { it != Long.MIN_VALUE && it > 0 }
                } catch (_: Throwable) { null },
            )
        }
    } catch (_: Throwable) {
        BatteryInfo()
    }
}
