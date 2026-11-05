package com.example.trampo.ui.profissional

import androidx.lifecycle.MutableLiveData
import com.example.trampo.data.model.*
import com.example.trampo.data.repository.EnderecoRepository
import com.example.trampo.data.repository.ProfissionalRepository
import com.example.trampo.ui.*

class ProfissionalLocaisViewModel(private val repository: ProfissionalRepository) : RequestViewModel() {
    val state = MutableLiveData<UiState<List<LocalResponse>>>(UiState.Idle)
    val removal = MutableLiveData<UiState<Unit>>(UiState.Idle)
    fun load() = request(state) { repository.locais() }
    fun remove(id: Int) = request(removal, mutation = true) {
        repository.excluirLocal(id)
        state.value = UiState.Success(repository.locais())
    }
}

class LocalEditorViewModel(private val repository: ProfissionalRepository,
    private val enderecos: EnderecoRepository) : RequestViewModel() {
    val state = MutableLiveData<UiState<LocalResponse?>>(UiState.Idle)
    val address = MutableLiveData<UiState<EnderecoResponse>>(UiState.Idle)
    val saved = MutableLiveData<UiState<Unit>>(UiState.Idle)
    fun load(id: Int) = request(state) { if (id == 0) null else repository.local(id) }
    fun cep(value: String) = request(address) { enderecos.cep(value) }
    fun save(id: Int, data: LocalRequest) = request(saved, mutation = true) {
        if (id == 0) repository.criarLocal(data) else repository.editarLocal(id, data)
        Unit
    }
}
