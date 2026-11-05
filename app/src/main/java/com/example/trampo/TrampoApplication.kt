package com.example.trampo

import android.app.Application
import com.example.trampo.data.SessionManager
import com.example.trampo.data.api.RetrofitClient
import com.example.trampo.data.repository.*

class TrampoApplication : Application() {
    val session by lazy { SessionManager(this) }
    private val api by lazy { RetrofitClient.create(session) }
    val conta by lazy { ContaRepository(api) }
    val notificacoes by lazy { NotificacaoRepository(api) }
    val auth by lazy { AuthRepository(api, session) }
    val servicos by lazy { ServicoRepository(api) }
    val agendamentos by lazy { AgendamentoRepository(api) }
    val enderecos by lazy { EnderecoRepository(api) }
    val profissional by lazy { ProfissionalRepository(api) }
}
