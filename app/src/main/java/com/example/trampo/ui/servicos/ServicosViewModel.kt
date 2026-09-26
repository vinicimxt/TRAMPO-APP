package com.example.trampo.ui.servicos

import androidx.lifecycle.MutableLiveData
import com.example.trampo.data.model.ServicoResponse
import com.example.trampo.data.repository.ServicoRepository
import com.example.trampo.ui.*

class ServicosViewModel(private val repository: ServicoRepository) : RequestViewModel() {
    val state = MutableLiveData<UiState<List<ServicoResponse>>>(UiState.Idle)
    private val items = mutableListOf<ServicoResponse>()
    var busca: String? = null; private set
    var atendimento: String? = null; private set
    fun search(text: String, modality: String?) {
        if (state.value is UiState.Loading) return
        busca = text.trim().ifBlank { null }; atendimento = modality
        load(true)
    }
    private var nextPage = 1
    var hasMore = true
        private set
    fun load(reset: Boolean = false) {
        if (state.value is UiState.Loading) return
        if (reset) { nextPage = 1; items.clear(); hasMore = true }
        if (!hasMore) return
        request(state) {
            val page = repository.listar(nextPage, busca, atendimento)
            items.addAll(page.filter { candidate -> items.none { it.id == candidate.id } })
            nextPage++
            hasMore = page.size == 20
            items.toList()
        }
    }
}
class DetalheServicoViewModel(private val repository: ServicoRepository) : RequestViewModel() {
    val state = MutableLiveData<UiState<ServicoResponse>>(UiState.Idle)
    fun load(id: Int) = request(state) { repository.buscar(id) }
}