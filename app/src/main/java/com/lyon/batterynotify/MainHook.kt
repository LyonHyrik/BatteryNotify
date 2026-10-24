package com.lyon.batterynotify

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.lyon.batterynotify.systemui.BatteryLog
import com.lyon.batterynotify.systemui.BatteryNotifier
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class MainHook : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        when (lpparam.packageName) {
            BatteryConstants.SYSTEMUI_PACKAGE -> {
                BatteryLog.x("handleLoadPackage: ${lpparam.packageName} / process=${lpparam.processName}, scheduling notifier start")
                try {
                    val nmClass = XposedHelpers.findClass("android.app.NotificationManager", lpparam.classLoader)
                    XposedHelpers.findAndHookMethod(nmClass, "canPostPromotedNotifications", object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            param.result = true
                            BatteryLog.x("forced canPostPromotedNotifications = true in SystemUI")
                        }
                    })
                    BatteryLog.x("hooked NotificationManager.canPostPromotedNotifications in SystemUI")
                } catch (t: Throwable) {
                    BatteryLog.x("hook NotificationManager.canPostPromotedNotifications in SystemUI failed", t)
                }
                hookStatusBarIcon(lpparam)
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
            // 系统框架 (android) hook 已移除：仅 hook SystemUI，避免安装后需软重启系统
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

    private fun hookStatusBarIcon(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val clazz = XposedHelpers.findClass(
                "com.android.systemui.statusbar.notification.icon.domain.interactor.NotificationIconsInteractor",
                lpparam.classLoader,
            )
            val method = try {
                XposedHelpers.findMethodExact(
                    clazz,
                    "access\$shouldShowNotificationIcon",
                    "com.android.systemui.statusbar.notification.icon.domain.interactor.NotificationIconsInteractor",
                    "com.android.systemui.statusbar.notification.shared.ActiveNotificationModel",
                    Boolean::class.javaPrimitiveType,
                    Boolean::class.javaPrimitiveType,
                    Boolean::class.javaPrimitiveType,
                    Boolean::class.javaPrimitiveType,
                    Boolean::class.javaPrimitiveType,
                    String::class.java,
                    Boolean::class.javaPrimitiveType,
                )
            } catch (_: Throwable) {
                val activeModel = XposedHelpers.findClass(
                    "com.android.systemui.statusbar.notification.shared.ActiveNotificationModel",
                    lpparam.classLoader,
                )
                XposedHelpers.findMethodBestMatch(
                    clazz,
                    "access\$shouldShowNotificationIcon",
                    clazz,
                    activeModel,
                    Boolean::class.javaPrimitiveType,
                    Boolean::class.javaPrimitiveType,
                    Boolean::class.javaPrimitiveType,
                    Boolean::class.javaPrimitiveType,
                    Boolean::class.javaPrimitiveType,
                    String::class.java,
                    Boolean::class.javaPrimitiveType,
                )
            }

            XposedBridge.hookMethod(method, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    try {
                        val app = currentApplication() ?: return
                        val prefs = app.getSharedPreferences("bn_systemui_cache", Context.MODE_PRIVATE)
                        val showStatusbarIcon = prefs.getBoolean(
                            BatteryConstants.KEY_SHOW_STATUSBAR_ICON,
                            BatteryConstants.DEFAULT_SHOW_STATUSBAR_ICON,
                        )
                        if (showStatusbarIcon) return

                        val model = param.args.getOrNull(1) ?: return
                        val key = try {
                            XposedHelpers.callMethod(model, "getKey") as? String
                        } catch (_: Throwable) {
                            try {
                                XposedHelpers.getObjectField(model, "key") as? String
                            } catch (_: Throwable) {
                                null
                            }
                        }

                        if (key != null && key.contains("|${BatteryConstants.SYSTEMUI_PACKAGE}|${BatteryConstants.NOTIFICATION_ID}|")) {
                            param.result = false
                            BatteryLog.x("hide statusbar icon for key=$key")
                        }
                    } catch (t: Throwable) {
                        BatteryLog.x("hookStatusBarIcon error", t)
                    }
                }
            })
            BatteryLog.x("hooked NotificationIconsInteractor.access\$shouldShowNotificationIcon")
        } catch (t: Throwable) {
            BatteryLog.x("hookStatusBarIcon failed", t)
        }
    }
}
