package com.example.trampo.ui.agendamentos

import androidx.lifecycle.MutableLiveData
import com.example.trampo.data.model.AgendamentoResponse
import com.example.trampo.data.model.EstadoAvaliacaoResponse
import com.example.trampo.data.repository.AgendamentoRepository
import com.example.trampo.ui.*

class AgendamentosViewModel(private val repository: AgendamentoRepository) : RequestViewModel() {
    val state = MutableLiveData<UiState<List<AgendamentoResponse>>>(UiState.Idle)
    fun load(visao: String = "meus") = request(state) { repository.listar(visao) }
}
class DetalheAgendamentoViewModel(private val repository: AgendamentoRepository) : RequestViewModel() {
    val state = MutableLiveData<UiState<AgendamentoResponse>>(UiState.Idle)
    val cancellation = MutableLiveData<UiState<Unit>>(UiState.Idle)
    val evaluation = MutableLiveData<UiState<EstadoAvaliacaoResponse>>(UiState.Idle)
    fun load(id: Int) = request(state) { repository.buscar(id) }
    fun loadEvaluation(id: Int) = request(evaluation) { repository.estadoAvaliacao(id) }
    fun complete(id: Int) = request(cancellation, mutation = true) {
        repository.concluir(id)
        state.value = UiState.Success(repository.buscar(id))
    }
    fun cancel(id: Int) = request(cancellation, mutation = true) {
        repository.cancelar(id)
        state.value = UiState.Success(repository.buscar(id))
    }
    fun confirm(id: Int) = request(cancellation, mutation = true) {
        repository.confirmar(id)
        state.value = UiState.Success(repository.buscar(id))
    }
    fun refuse(id: Int) = request(cancellation, mutation = true) {
        repository.recusar(id)
        state.value = UiState.Success(repository.buscar(id))
    }
    fun finalize(id: Int, value: java.math.BigDecimal) = request(cancellation, mutation = true) {
        repository.finalizar(id, value)
        state.value = UiState.Success(repository.buscar(id))
    }
}
