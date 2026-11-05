package com.example.trampo.ui.profissional

import androidx.lifecycle.MutableLiveData
import com.example.trampo.data.model.*
import com.example.trampo.data.repository.ProfissionalRepository
import com.example.trampo.ui.*

class ProfissionalServicosViewModel(private val repository: ProfissionalRepository) : RequestViewModel() {
    val state = MutableLiveData<UiState<List<ServicoProprioResponse>>>(UiState.Idle)
    val removal = MutableLiveData<UiState<Unit>>(UiState.Idle)
    fun load() = request(state) { repository.servicos() }
    fun remove(id: Int) = request(removal, mutation = true) {
        repository.removerServico(id)
        state.value = UiState.Success(repository.servicos())
    }
}

data class ServicoEditorData(val servicos: List<ServicoProprioResponse>,
    val categorias: List<CategoriaResponse>, val locais: List<LocalResponse>)

class ServicoEditorViewModel(private val repository: ProfissionalRepository) : RequestViewModel() {
    val state = MutableLiveData<UiState<ServicoEditorData>>(UiState.Idle)
    val saved = MutableLiveData<UiState<Unit>>(UiState.Idle)
    fun load() = request(state) { ServicoEditorData(repository.servicos(), repository.categorias(), repository.locais()) }
    fun save(id: Int, request: ServicoRequest) = request(saved, mutation = true) {
        if (id == 0) repository.criarServico(request) else repository.editarServico(id, request)
        Unit
    }
}
