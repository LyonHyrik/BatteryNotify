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
            "android" -> {
                XposedBridge.log("[BatteryNotify] ANDROID pkg=${lpparam.packageName} proc=${lpparam.processName}")
                try {
                    hookSystemServerPromotedPermission(lpparam)
                } catch (t: Throwable) {
                    XposedBridge.log("[BatteryNotify] hook system_server failed: $t")
                    BatteryLog.x("hook system_server failed", t)
                }
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

    private fun hookSystemServerPromotedPermission(lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedBridge.log("[BatteryNotify] hooking system_server for POST_PROMOTED_NOTIFICATIONS pid=${android.os.Process.myPid()}")
        val cl = lpparam.classLoader
        val systemUiPkg = BatteryConstants.SYSTEMUI_PACKAGE
        val promotedPerm = "android.permission.POST_PROMOTED_NOTIFICATIONS"
        val promotedOpStr = "android:post_promoted_notifications"

        // dump some classes to see if loaded
        try { XposedHelpers.findClass("com.android.server.appop.AppOpsService", cl); XposedBridge.log("[BatteryNotify] AppOpsService found") } catch (t: Throwable) { XposedBridge.log("[BatteryNotify] AppOpsService NOT found: $t") }
        try { XposedHelpers.findClass("com.android.server.notification.NotificationManagerService", cl); XposedBridge.log("[BatteryNotify] NMS found") } catch (t: Throwable) { XposedBridge.log("[BatteryNotify] NMS NOT found: $t") }
        try { XposedHelpers.findClass("com.android.server.pm.permission.PermissionManagerService", cl); XposedBridge.log("[BatteryNotify] PermissionManagerService found") } catch (t: Throwable) { XposedBridge.log("[BatteryNotify] PermissionManagerService NOT found: $t") }

        // Framework NotificationManager.canPostPromotedNotifications() 兜底
        try {
            val nmClass = XposedHelpers.findClass("android.app.NotificationManager", cl)
            for (m in nmClass.declaredMethods) {
                if (m.name == "canPostPromotedNotifications") {
                    try {
                        XposedBridge.hookMethod(m, object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                try {
                                    val callingUid = android.os.Binder.getCallingUid()
                                    if (callingUid == 10256 || callingUid % 100000 == 10256) {
                                        param.result = true
                                        XposedBridge.log("[BatteryNotify] NotificationManager.canPostPromotedNotifications -> true for SystemUI uid=$callingUid")
                                    }
                                } catch (_: Throwable) {
                                }
                            }
                        })
                        XposedBridge.log("[BatteryNotify] hooked android.app.NotificationManager.canPostPromotedNotifications")
                    } catch (_: Throwable) {
                    }
                    break
                }
            }
        } catch (t: Throwable) {
            XposedBridge.log("[BatteryNotify] hook framework NotificationManager failed: $t")
        }

        // AppOpsService
        try {
            val appOpsClass = XposedHelpers.findClass("com.android.server.appop.AppOpsService", cl)
            val hook = object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        val pkg = param.args.firstOrNull { it is String && it.startsWith("com.") } as? String
                        if (pkg != systemUiPkg) return
                        for (arg in param.args) {
                            if (arg is String && (arg == promotedPerm || arg == promotedOpStr || arg.contains("POST_PROMOTED"))) {
                                param.result = 0
                                XposedBridge.log("[BatteryNotify] AppOpsService allowed POST_PROMOTED_NOTIFICATIONS for SystemUI")
                                return
                            }
                            if (arg is Int) {
                                try {
                                    val appOpsManagerClass = XposedHelpers.findClass("android.app.AppOpsManager", cl)
                                    val opstr = XposedHelpers.callStaticMethod(appOpsManagerClass, "opToName", arg) as? String
                                    if (opstr != null && opstr.contains("POST_PROMOTED")) {
                                        param.result = 0
                                        XposedBridge.log("[BatteryNotify] AppOpsService allowed op=$arg ($opstr) for SystemUI")
                                        return
                                    }
                                } catch (_: Throwable) {
                                }
                            }
                        }
                    } catch (_: Throwable) {
                    }
                }
            }
            for (m in appOpsClass.declaredMethods) {
                if (m.name == "checkOperation" || m.name == "checkOperationInternal" || m.name == "noteOperation" || m.name == "startOperation" || m.name == "noteProxyOperation") {
                    try { XposedBridge.hookMethod(m, hook) } catch (_: Throwable) {}
                }
            }
        } catch (t: Throwable) {
            XposedBridge.log("[BatteryNotify] hook AppOpsService failed: $t")
        }

        // AppOpsManager
        try {
            val appOpsManagerClass = XposedHelpers.findClass("android.app.AppOpsManager", cl)
            val hook = object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        val pkg = param.args.firstOrNull { it is String && it == systemUiPkg } as? String ?: return
                        for (arg in param.args) {
                            if (arg is Int) {
                                try {
                                    val opstr = XposedHelpers.callStaticMethod(appOpsManagerClass, "opToName", arg) as? String
                                    if (opstr != null && opstr.contains("POST_PROMOTED")) {
                                        param.result = 0
                                        XposedBridge.log("[BatteryNotify] AppOpsManager allowed op=$arg for SystemUI")
                                        return
                                    }
                                } catch (_: Throwable) {
                                }
                            }
                            if (arg is String && arg == promotedOpStr) {
                                param.result = 0
                                XposedBridge.log("[BatteryNotify] AppOpsManager allowed opstr for SystemUI")
                                return
                            }
                        }
                    } catch (_: Throwable) {
                    }
                }
            }
            for (m in appOpsManagerClass.declaredMethods) {
                if (m.name == "checkOp" || m.name == "checkOpNoThrow" || m.name == "noteOp" || m.name == "noteOpNoThrow") {
                    try { XposedBridge.hookMethod(m, hook) } catch (_: Throwable) {}
                }
            }
        } catch (t: Throwable) {
            XposedBridge.log("[BatteryNotify] hook AppOpsManager failed: $t")
        }

        // PermissionManagerService
        try {
            val pmServiceClass = XposedHelpers.findClass("com.android.server.pm.permission.PermissionManagerService", cl)
            for (m in pmServiceClass.declaredMethods) {
                if (!m.name.contains("checkPermission")) continue
                try {
                    XposedBridge.hookMethod(m, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            try {
                                for (arg in param.args) {
                                    if (arg == promotedPerm) {
                                        var isSystemUi = false
                                        for (a in param.args) {
                                            if (a is String && a == systemUiPkg) { isSystemUi = true; break }
                                            if (a is Int && a == 10256) { isSystemUi = true; break }
                                        }
                                        if (isSystemUi) {
                                            param.result = 0
                                            XposedBridge.log("[BatteryNotify] granted POST_PROMOTED_NOTIFICATIONS to SystemUI via $m.name")
                                            return
                                        }
                                        break
                                    }
                                }
                            } catch (_: Throwable) {
                            }
                        }
                    })
                } catch (_: Throwable) {
                }
            }
        } catch (t: Throwable) {
            XposedBridge.log("[BatteryNotify] hook pm failed: $t")
        }

        // NMS
        try {
            val nmsClass = XposedHelpers.findClass("com.android.server.notification.NotificationManagerService", cl)
            for (m in nmsClass.declaredMethods) {
                if (m.name == "canPostPromotedNotifications" || m.name.contains("PromotedNotifications")) {
                    try {
                        XposedBridge.hookMethod(m, object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                try {
                                    for (arg in param.args) {
                                        if (arg is String && arg == systemUiPkg) {
                                            param.result = true
                                            XposedBridge.log("[BatteryNotify] NMS.canPostPromotedNotifications -> true for SystemUI")
                                            return
                                        }
                                    }
                                    for (arg in param.args) {
                                        if (arg is Int && arg == 10256) {
                                            param.result = true
                                            XposedBridge.log("[BatteryNotify] NMS.canPostPromotedNotifications -> true for SystemUI uid")
                                            return
                                        }
                                    }
                                } catch (_: Throwable) {
                                }
                            }
                        })
                    } catch (_: Throwable) {
                    }
                }
            }
        } catch (t: Throwable) {
            XposedBridge.log("[BatteryNotify] hook NMS promoted failed: $t")
        }
    }
}
