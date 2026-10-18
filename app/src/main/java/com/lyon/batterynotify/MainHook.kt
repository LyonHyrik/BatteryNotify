package com.lyon.batterynotify

import android.content.Context
import android.os.Handler
import android.os.Looper
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage
import com.lyon.batterynotify.systemui.BatteryLog
import com.lyon.batterynotify.systemui.BatteryNotifier

class MainHook : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        when (lpparam.packageName) {
            BatteryConstants.SYSTEMUI_PACKAGE -> {
                BatteryLog.x("handleLoadPackage: ${lpparam.packageName} / process=${lpparam.processName}, scheduling notifier start")
                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        val app = currentApplication()
                        if (app == null) {
                            BatteryLog.x("currentApplication() is null, abort")
                            return@postDelayed
                        }
                        reportModuleVersion(app)
                        BatteryNotifier.start(app)
                        BatteryLog.x("notifier started in ${app.packageName}")
                    } catch (t: Throwable) {
                        BatteryLog.x("SystemUI init failed", t)
                    }
                }, 3000)
            }
        }
    }

    private fun currentApplication(): Context? = try {
        Class.forName("android.app.ActivityThread")
            .getMethod("currentApplication")
            .invoke(null) as Context?
    } catch (t: Throwable) {
        null
    }

    private fun reportModuleVersion(app: Context) {
        try {
            val bundle = android.os.Bundle()
            app.contentResolver.call(
                BatteryConstants.PROVIDER_URI,
                BatteryConstants.METHOD_SET_MODULE_VERSION,
                BuildConfig.VERSION_CODE.toString(),
                bundle,
            )
            BatteryLog.x("module version reported: ${BuildConfig.VERSION_CODE}")
        } catch (t: Throwable) {
            BatteryLog.x("failed to report module version: $t")
        }
    }
}
