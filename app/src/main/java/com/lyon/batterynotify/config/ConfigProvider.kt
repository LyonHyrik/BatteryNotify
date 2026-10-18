package com.lyon.batterynotify.config

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import com.lyon.batterynotify.BatteryConstants

class ConfigProvider : ContentProvider() {

    private var store: ConfigStore? = null

    override fun onCreate(): Boolean {
        val appCtx = context?.applicationContext
        store = appCtx?.let { ConfigStore(it) }
        return store != null
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        when (method) {
            "get_config" -> {
                val config = store?.read() ?: BatteryConfig()
                return config.toBundle()
            }
            BatteryConstants.METHOD_SET_MODULE_VERSION -> {
                val prefs = context?.getSharedPreferences(BatteryConstants.PREF_NAME, Context.MODE_PRIVATE)
                prefs?.edit()?.putInt(BatteryConstants.KEY_MODULE_VERSION, arg?.toIntOrNull() ?: 0)?.apply()
                return Bundle()
            }
            BatteryConstants.METHOD_GET_MODULE_VERSION -> {
                val prefs = context?.getSharedPreferences(BatteryConstants.PREF_NAME, Context.MODE_PRIVATE)
                val version = prefs?.getInt(BatteryConstants.KEY_MODULE_VERSION, 0) ?: 0
                return Bundle().apply { putInt(BatteryConstants.KEY_MODULE_VERSION, version) }
            }
        }
        return null
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
