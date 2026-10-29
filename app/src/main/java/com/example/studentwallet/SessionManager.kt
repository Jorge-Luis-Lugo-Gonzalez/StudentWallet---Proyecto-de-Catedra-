package com.example.studentwallet

import android.content.Context

class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences("student_wallet_prefs", Context.MODE_PRIVATE)

    fun saveSession(userId: Long, email: String) {
        prefs.edit()
            .putLong("user_id", userId)
            .putString("user_email", email)
            .putBoolean("is_logged_in", true)
            .apply()
    }

    fun getUserId(): Long = prefs.getLong("user_id", 1L)

    fun isLoggedIn(): Boolean = prefs.getBoolean("is_logged_in", false)

    fun areNotificationsEnabled(): Boolean = prefs.getBoolean("notifications_enabled", true)

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
    }

    fun clearSession() {
        prefs.edit()
            .remove("user_id")
            .remove("user_email")
            .putBoolean("is_logged_in", false)
            .apply()
    }
}
