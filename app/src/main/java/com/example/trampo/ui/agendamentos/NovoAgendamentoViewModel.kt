package com.example.trampo.ui.agendamentos

import androidx.lifecycle.MutableLiveData
import com.example.trampo.data.model.*
import com.example.trampo.data.repository.*
import com.example.trampo.ui.*

class NovoAgendamentoViewModel(
    private val servicos: ServicoRepository,
    private val agendamentos: AgendamentoRepository,
    private val enderecos: EnderecoRepository
) : RequestViewModel() {
    val service = MutableLiveData<UiState<ServicoResponse>>(UiState.Idle)
    val agenda = MutableLiveData<UiState<AgendaResponse>>(UiState.Idle)
    val address = MutableLiveData<UiState<EnderecoResponse>>(UiState.Idle)
    val creation = MutableLiveData<UiState<RecursoCriadoResponse>>(UiState.Idle)
    var draft: Map<Int, String> = emptyMap()
    var selectedDate = ""
        private set
    var selectedTime: String? = null
    var reviewed: AgendamentoRequest? = null
        private set
    var reviewedSummary: String? = null
        private set
    var addressQuery = ""
        private set

    fun load(id: Int) = request(service) { servicos.buscar(id) }
    fun dates(id: Int, date: String) {
        if (agenda.value is UiState.Loading || creation.value is UiState.Loading) return
        selectedDate = date
        selectedTime = null
        discardReview()
        request(agenda) { servicos.agenda(id, date) }
    }
    fun cep(value: String) {
        if (address.value is UiState.Loading) return
        addressQuery = value.filter(Char::isDigit)
        request(address) { enderecos.cep(addressQuery) }
    }
    fun consumeAddress() { address.value = UiState.Idle }
    fun review(value: AgendamentoRequest, summary: String) {
        reviewed = value
        reviewedSummary = summary
    }
    fun discardReview() { reviewed = null; reviewedSummary = null }
    fun create() {
        val payload = reviewed ?: return
        if (creation.value is UiState.Success || (creation.value as? UiState.Error)?.failure?.uncertain == true) return
        request(creation, mutation = true) { agendamentos.criar(payload) }
    }
    fun consumeCreated() { creation.value = UiState.Idle; discardReview() }
}