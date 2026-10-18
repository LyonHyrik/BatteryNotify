package com.lyon.batterynotify.systemui

import de.robv.android.xposed.XposedBridge

object BatteryLog {
    private const val TAG = "BatteryNotify"

    fun x(msg: String) {
        XposedBridge.log("[$TAG] $msg")
    }

    fun x(msg: String, t: Throwable) {
        XposedBridge.log("[$TAG] $msg: $t")
        XposedBridge.log(t)
    }
}
