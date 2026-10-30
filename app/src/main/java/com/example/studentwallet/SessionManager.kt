package com.example.studentwallet

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

/**
 * La sesión ahora la maneja Firebase Authentication.
 * SharedPreferences solo guarda preferencias locales (notificaciones).
 */
class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences("student_wallet_prefs", Context.MODE_PRIVATE)
    private val auth = FirebaseAuth.getInstance()

    fun getUserId(): String = auth.currentUser?.uid ?: ""

    fun getUserEmail(): String = auth.currentUser?.email ?: ""

    fun isLoggedIn(): Boolean = auth.currentUser != null

    fun areNotificationsEnabled(): Boolean = prefs.getBoolean("notifications_enabled", true)

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
    }

    fun clearSession() {
        auth.signOut()
    }
}
