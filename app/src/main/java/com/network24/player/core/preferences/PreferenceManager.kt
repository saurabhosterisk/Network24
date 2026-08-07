package com.network24.player.core.preferences

import android.content.Context
import android.content.SharedPreferences
import com.network24.player.common.models.LoginCredentials

class PreferenceManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("network24", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SERVER = "server"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_REMEMBER = "remember"

        private const val KEY_STATUS = "status"
        private const val KEY_EXPIRY = "expiry"
        private const val KEY_ACTIVE_CONNECTIONS = "active_connections"
        private const val KEY_MAX_CONNECTIONS = "max_connections"
        private const val KEY_IS_TRIAL = "is_trial"
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

    fun saveUserInfo(
        username: String,
        status: String,
        expiry: Long,
        activeConnections: Int,
        maxConnections: Int,
        isTrial: Boolean
    ) {

        prefs.edit()
            .putString(KEY_STATUS, status)
            .putLong(KEY_EXPIRY, expiry)
            .putInt(KEY_ACTIVE_CONNECTIONS, activeConnections)
            .putInt(KEY_MAX_CONNECTIONS, maxConnections)
            .putBoolean(KEY_IS_TRIAL, isTrial)
            .apply()

    }

    fun getStatus() =
        prefs.getString(KEY_STATUS, "Unknown") ?: "Unknown"

    fun getExpiry() =
        prefs.getLong(KEY_EXPIRY, 0L)

    fun getActiveConnections() =
        prefs.getInt(KEY_ACTIVE_CONNECTIONS, 0)

    fun getMaxConnections() =
        prefs.getInt(KEY_MAX_CONNECTIONS, 0)

    fun isTrial() =
        prefs.getBoolean(KEY_IS_TRIAL, false)

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun getCredentials(): LoginCredentials {

        return LoginCredentials(
            server = getServer(),
            username = getUsername(),
            password = getPassword()
        )

    }

    // Sync time save karne ke liye
    fun setLastSyncTime(time: Long) {
        prefs.edit().putLong("last_sync_time", time).apply()
    }

    // Sync time nikalne ke liye
    fun getLastSyncTime(): Long {
        return prefs.getLong("last_sync_time", 0L)
    }

    // =========================
// Chat preferences
// =========================
    private val KEY_CHAT_LAST_ROOM_ID = "chat_last_room_id"

    fun setLastChatRoomId(roomId: String) {
        prefs.edit().putString(KEY_CHAT_LAST_ROOM_ID, roomId).apply()
    }

    fun getLastChatRoomId(): String? {
        return prefs.getString(KEY_CHAT_LAST_ROOM_ID, null)
    }

    private fun chatLastSeenKey(roomId: String) = "chat_last_seen_$roomId"

    fun setChatLastSeen(roomId: String, tsMs: Long) {
        prefs.edit().putLong(chatLastSeenKey(roomId), tsMs).apply()
    }

    fun getChatLastSeen(roomId: String): Long {
        return prefs.getLong(chatLastSeenKey(roomId), 0L)
    }


}