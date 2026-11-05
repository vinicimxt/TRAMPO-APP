package com.example.trampo.data.repository

import com.example.trampo.data.TokenStore
import com.example.trampo.data.api.TrampoApi
import com.example.trampo.data.model.*

class PerfilNaoSuportado : Exception()
class RespostaInvalida : Exception()

class AuthRepository(private val api: TrampoApi, private val session: TokenStore) {
    suspend fun register(request: CadastroRequest): UsuarioResponse {
        val resposta = api.register(request)
        if (resposta.accessToken.isNullOrBlank() || resposta.tokenType != "Bearer") throw RespostaInvalida()
        session.save(resposta.accessToken)
        return me()
    }
    fun hasSession() = !session.token().isNullOrBlank()
    suspend fun login(email: String, senha: String): UsuarioResponse {
        val resposta = api.login(LoginRequest(email, senha))
        if (resposta.accessToken.isNullOrBlank() || resposta.tokenType != "Bearer") throw RespostaInvalida()
        session.save(resposta.accessToken)
        return me()
    }
    suspend fun me(): UsuarioResponse {
        val user = api.me()
        if (user.id <= 0 || user.nome.isNullOrBlank()) throw RespostaInvalida()
        if (user.tipo != "cliente" && user.tipo != "profissional") {
            logout()
            throw PerfilNaoSuportado()
        }
        session.saveUser(user)
        return user
    }
    fun logout() = session.clear()
}
class ServicoRepository(private val api: TrampoApi) {
    suspend fun listar(page: Int, busca: String? = null, atendimento: String? = null) = api.servicos(page, busca = busca, atendimento = atendimento)
    suspend fun buscar(id: Int) = api.servico(id)
    suspend fun agenda(id: Int, data: String) = api.agenda(id, data)
    suspend fun avaliacoes(id: Int) = api.avaliacoesPublicas(id)
}
class AgendamentoRepository(private val api: TrampoApi) {
    suspend fun listar(visao: String = "meus") = api.agendamentos(visao)
    suspend fun buscar(id: Int) = api.agendamento(id)
    suspend fun criar(request: AgendamentoRequest) = api.criar(request)
    suspend fun concluir(id: Int) = api.concluir(id)
    suspend fun estadoAvaliacao(id: Int) = api.estadoAvaliacao(id)
    suspend fun avaliar(id: Int, request: AvaliacaoRequest) = api.avaliar(id, request)
    suspend fun cancelar(id: Int) = api.cancelar(id)
    suspend fun confirmar(id: Int) = api.confirmar(id)
    suspend fun recusar(id: Int) = api.recusar(id)
    suspend fun finalizar(id: Int, valor: java.math.BigDecimal) = api.finalizar(id, com.example.trampo.data.api.FinalizarRequest(valor))
}
class EnderecoRepository(private val api: TrampoApi) {
    suspend fun cep(cep: String) = api.cep(cep.filter(Char::isDigit))
}
class ContaRepository(private val api: TrampoApi) {
    suspend fun perfil() = api.perfil()
    suspend fun salvar(request: PerfilRequest) = api.salvarPerfil(request)
    suspend fun senha(request: SenhaRequest) = api.senha(request)
    suspend fun suporte(request: SuporteRequest) = api.suporte(request)
}
class NotificacaoRepository(private val api: TrampoApi) {
    suspend fun listar(page: Int) = api.notificacoes(page)
    suspend fun ler(id: Int) = api.ler(id)
}
class ProfissionalRepository(private val api: TrampoApi) {
    suspend fun servicos() = api.servicosProprios()
    suspend fun categorias() = api.categorias()
    suspend fun criarServico(request: ServicoRequest) = api.criarServico(request)
    suspend fun editarServico(id: Int, request: ServicoRequest) = api.editarServico(id, request)
    suspend fun removerServico(id: Int) = api.removerServico(id)
    suspend fun locais() = api.locais()
    suspend fun local(id: Int) = api.local(id)
    suspend fun criarLocal(request: LocalRequest) = api.criarLocal(request)
    suspend fun editarLocal(id: Int, request: LocalRequest) = api.editarLocal(id, request)
    suspend fun excluirLocal(id: Int) = api.excluirLocal(id)
    suspend fun avaliacoes() = api.avaliacoesRecebidas()
}
