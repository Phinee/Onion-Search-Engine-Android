package com.onionsearchengine.onionsearchengine

import android.content.Context
import android.content.SharedPreferences

class ApiKeyManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val API_KEY = "api_key"
    }

    fun saveApiKey(key: String) {
        prefs.edit().putString(API_KEY, key).apply()
    }

    fun getApiKey(): String? {
        return prefs.getString(API_KEY, null)
    }
}