package com.example.trampo.ui.agendamentos

import androidx.lifecycle.MutableLiveData
import com.example.trampo.data.model.AgendamentoResponse
import com.example.trampo.data.repository.AgendamentoRepository
import com.example.trampo.ui.*

class AgendamentosViewModel(private val repository: AgendamentoRepository) : RequestViewModel() {
    val state = MutableLiveData<UiState<List<AgendamentoResponse>>>(UiState.Idle)
    fun load() = request(state) { repository.listar() }
}
class DetalheAgendamentoViewModel(private val repository: AgendamentoRepository) : RequestViewModel() {
    val state = MutableLiveData<UiState<AgendamentoResponse>>(UiState.Idle)
    val cancellation = MutableLiveData<UiState<Unit>>(UiState.Idle)
    fun load(id: Int) = request(state) { repository.buscar(id) }
    fun complete(id: Int) = request(cancellation, mutation = true) {
        repository.concluir(id)
        state.value = UiState.Success(repository.buscar(id))
    }
    fun cancel(id: Int) = request(cancellation, mutation = true) {
        repository.cancelar(id)
        state.value = UiState.Success(repository.buscar(id))
    }
}