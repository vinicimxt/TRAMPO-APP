package com.example.trampo.ui.login

import androidx.lifecycle.MutableLiveData
import com.example.trampo.data.model.UsuarioResponse
import com.example.trampo.data.repository.AuthRepository
import com.example.trampo.ui.*

class LoginViewModel(private val repository: AuthRepository) : RequestViewModel() {
    val state = MutableLiveData<UiState<UsuarioResponse>>(UiState.Idle)
    fun login(email: String, senha: String) = request(state, login = true) { repository.login(email, senha) }
}