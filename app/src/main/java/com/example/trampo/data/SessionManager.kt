package com.example.trampo.data

import android.content.Context
import androidx.core.content.edit
import com.example.trampo.data.model.UsuarioResponse

data class SessionUser(val id: Int, val nome: String, val tipo: String)

interface TokenStore {
    fun token(): String?
    fun save(token: String)
    fun clear()
    fun clearIfMatches(token: String)
    fun user(): SessionUser? = null
    fun saveUser(user: UsuarioResponse) = Unit
}

// JWT e perfil mínimo em armazenamento privado; excluídos de backup e transferência.
class SessionManager(context: Context) : TokenStore {
    private val prefs = context.applicationContext.getSharedPreferences("trampo_session", Context.MODE_PRIVATE)
    @Synchronized override fun token(): String? = prefs.getString("access_token", null)
    @Synchronized override fun save(token: String) {
        prefs.edit { putString("access_token", token); remove("user_id"); remove("user_name"); remove("user_type") }
    }
    @Synchronized override fun user(): SessionUser? {
        if (token().isNullOrBlank()) return null
        val id = prefs.getInt("user_id", 0)
        val name = prefs.getString("user_name", null)
        val type = prefs.getString("user_type", null)
        return if (id > 0 && !name.isNullOrBlank() && type in setOf("cliente", "profissional"))
            SessionUser(id, name, type!!) else null
    }
    @Synchronized override fun saveUser(user: UsuarioResponse) {
        if (!token().isNullOrBlank()) prefs.edit {
            putInt("user_id", user.id); putString("user_name", user.nome); putString("user_type", user.tipo)
        }
    }
    @Synchronized override fun clear() { prefs.edit { clear() } }
    @Synchronized override fun clearIfMatches(token: String) {
        if (token() == token) clear()
    }
}
