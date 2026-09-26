package com.example.trampo.ui.conta

import androidx.lifecycle.MutableLiveData
import com.example.trampo.data.model.*
import com.example.trampo.data.repository.*
import com.example.trampo.ui.*

class CadastroViewModel(private val repository: AuthRepository) : RequestViewModel() {
    val state = MutableLiveData<UiState<UsuarioResponse>>(UiState.Idle)
    fun enviar(dados: CadastroRequest) {
        if (state.value is UiState.Success || (state.value as? UiState.Error)?.failure?.uncertain == true) return
        request(state, mutation = true) { repository.register(dados) }
    }
}
class PerfilViewModel(private val repository: ContaRepository) : RequestViewModel() {
    val state = MutableLiveData<UiState<PerfilResponse>>(UiState.Idle)
    val saved = MutableLiveData<UiState<Unit>>(UiState.Idle)
    fun load() = request(state) { repository.perfil() }
    fun salvar(dados: PerfilRequest) = request(saved, mutation = true) { repository.salvar(dados) }
}
class SenhaViewModel(private val repository: ContaRepository) : RequestViewModel() {
    val state = MutableLiveData<UiState<Unit>>(UiState.Idle)
    fun enviar(dados: SenhaRequest) {
        if (state.value is UiState.Success || (state.value as? UiState.Error)?.failure?.uncertain == true) return
        request(state, mutation = true) { repository.senha(dados) }
    }
}
class SuporteViewModel(private val repository: ContaRepository) : RequestViewModel() {
    val state = MutableLiveData<UiState<Unit>>(UiState.Idle)
    fun enviar(dados: SuporteRequest) {
        if (state.value is UiState.Success || (state.value as? UiState.Error)?.failure?.uncertain == true) return
        request(state, mutation = true) { repository.suporte(dados) }
    }
}
class NotificacoesViewModel(private val repository: NotificacaoRepository) : RequestViewModel() {
    val state = MutableLiveData<UiState<List<NotificacaoResponse>>>(UiState.Idle)
    private val items = mutableListOf<NotificacaoResponse>()
    private var page = 1
    var hasMore = true; private set
    fun load(reset: Boolean = false) {
        if (state.value is UiState.Loading) return
        if (reset) { items.clear(); page = 1; hasMore = true }
        if (!hasMore) return
        request(state) {
            val result = repository.listar(page)
            items.addAll(result.filter { n -> items.none { it.id == n.id } })
            page++; hasMore = result.size == 20
            items.toList()
        }
    }
    fun ler(id: Int) = request(state, mutation = true) {
        repository.ler(id)
        val index = items.indexOfFirst { it.id == id }
        if (index >= 0) items[index] = items[index].copy(lida = true)
        items.toList()
    }
}
