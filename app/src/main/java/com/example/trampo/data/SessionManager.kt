package com.example.trampo.data

import android.content.Context
import androidx.core.content.edit

interface TokenStore {
    fun token(): String?
    fun save(token: String)
    fun clear()
    fun clearIfMatches(token: String)
}

// Somente JWT, em armazenamento privado; excluído de backup e transferência.
class SessionManager(context: Context) : TokenStore {
    private val prefs = context.applicationContext.getSharedPreferences("trampo_session", Context.MODE_PRIVATE)
    @Synchronized override fun token(): String? = prefs.getString("access_token", null)
    @Synchronized override fun save(token: String) { prefs.edit { putString("access_token", token) } }
    @Synchronized override fun clear() { prefs.edit { clear() } }
    @Synchronized override fun clearIfMatches(token: String) {
        if (token() == token) clear()
    }
}