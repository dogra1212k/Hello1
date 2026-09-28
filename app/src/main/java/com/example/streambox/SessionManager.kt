package com.example.streambox

import android.content.Context

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)
    fun login(email: String, admin: Boolean = false) = prefs.edit().putBoolean("logged_in", true).putString("email", email).putBoolean("admin", admin).apply()
    fun logout() = prefs.edit().clear().apply()
    fun isLoggedIn() = prefs.getBoolean("logged_in", false)
    fun isAdmin() = prefs.getBoolean("admin", false)
    fun email() = prefs.getString("email", "") ?: ""
}