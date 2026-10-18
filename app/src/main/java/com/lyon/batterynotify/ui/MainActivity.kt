package com.lyon.batterynotify.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.lyon.batterynotify.R
import com.lyon.batterynotify.config.ConfigStore
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.Settings

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val store = remember { ConfigStore(applicationContext) }
            var monetEnabled by rememberSaveable { mutableStateOf(store.read().monet) }
            AppTheme(monetEnabled = monetEnabled) {
                AppNav(store = store, onMonetChange = { monetEnabled = it })
            }
        }
    }
}

private enum class Screen {
    Home,
    Settings,
}

@Composable
private fun AppNav(
    store: ConfigStore,
    onMonetChange: (Boolean) -> Unit,
) {
    var screen by rememberSaveable { mutableStateOf(Screen.Home) }

    BackHandler(enabled = screen != Screen.Settings) {
        screen = Screen.Home
    }

    when (screen) {
        Screen.Home -> MainScreen(
            bottomBar = { MiuixBottomBar(selected = Screen.Home) { screen = it } },
        )

        Screen.Settings -> SettingsScreen(
            store = store,
            onMonetChange = onMonetChange,
            bottomBar = { MiuixBottomBar(selected = Screen.Settings) { screen = it } },
        )
    }
}

@Composable
private fun MiuixBottomBar(
    selected: Screen,
    onSelect: (Screen) -> Unit,
) {
    NavigationBar {
        NavigationBarItem(
            selected = selected == Screen.Home,
            onClick = { onSelect(Screen.Home) },
            icon = MiuixIcons.GridView,
            label = stringResource(R.string.nav_home),
        )
        NavigationBarItem(
            selected = selected == Screen.Settings,
            onClick = { onSelect(Screen.Settings) },
            icon = MiuixIcons.Settings,
            label = stringResource(R.string.nav_settings),
        )
    }
}
