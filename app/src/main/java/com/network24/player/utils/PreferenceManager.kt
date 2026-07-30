package com.network24.player.utils

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("network24", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SERVER = "server"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_REMEMBER = "remember"
    }

    fun saveLogin(
        server: String,
        username: String,
        password: String,
        remember: Boolean
    ) {
        prefs.edit()
            .putString(KEY_SERVER, server)
            .putString(KEY_USERNAME, username)
            .putString(KEY_PASSWORD, password)
            .putBoolean(KEY_REMEMBER, remember)
            .apply()
    }

    fun getServer() = prefs.getString(KEY_SERVER, "") ?: ""

    fun getUsername() = prefs.getString(KEY_USERNAME, "") ?: ""

    fun getPassword() = prefs.getString(KEY_PASSWORD, "") ?: ""

    fun isRememberMe() = prefs.getBoolean(KEY_REMEMBER, false)

    fun clear() {
        prefs.edit().clear().apply()
    }
}