package com.example.trampo.ui.home

import androidx.lifecycle.MutableLiveData
import com.example.trampo.data.model.UsuarioResponse
import com.example.trampo.data.repository.AuthRepository
import com.example.trampo.ui.*

class HomeViewModel(private val repository: AuthRepository) : RequestViewModel() {
    val state = MutableLiveData<UiState<UsuarioResponse>>(UiState.Idle)
    fun hasSession() = repository.hasSession()
    fun load() = request(state) { repository.me() }
    fun logout() = repository.logout()
}