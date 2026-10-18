# BatteryNotify keep rules
-keep class com.lyon.batterynotify.MainHook { *; }
-keep class com.lyon.batterynotify.systemui.** { *; }
-dontwarn de.robv.android.xposed.**
-dontwarn org.jetbrains.annotations.**
