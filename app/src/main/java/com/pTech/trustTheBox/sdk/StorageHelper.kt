package com.pTech.trustTheBox.sdk

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

private const val PREFS_NAME = "app_storage"
private lateinit var sharedPreferences: SharedPreferences

fun initStorage(context: Context) {
    sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

fun setValue(key: String, value: Any) {
    sharedPreferences.edit {
        when (value) {
            is String -> putString(key, value)
            is Int -> putInt(key, value)
            is Long -> putLong(key, value)
            is Float -> putFloat(key, value)
            is Boolean -> putBoolean(key, value)
            else -> throw IllegalArgumentException("Unsupported type for SharedPreferences")
        }
    }
}

fun getValue(key: String, defaultValue: Any? = null): Any? {
    return when (defaultValue) {
        is String? -> sharedPreferences.getString(key, defaultValue)
        is Int -> sharedPreferences.getInt(key, defaultValue)
        is Long -> sharedPreferences.getLong(key, defaultValue)
        is Float -> sharedPreferences.getFloat(key, defaultValue)
        is Boolean -> sharedPreferences.getBoolean(key, defaultValue)
        else -> throw IllegalArgumentException("Unsupported type for SharedPreferences")
    }
}
