package com.example.trampo

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.trampo.data.TokenStore
import com.example.trampo.data.api.*
import com.example.trampo.data.model.*
import com.example.trampo.data.repository.*
import com.example.trampo.ui.*
import com.example.trampo.ui.agendamentos.NovoAgendamentoViewModel
import com.example.trampo.ui.servicos.ServicosViewModel
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.*
import org.junit.*
import org.junit.Assert.*
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.SocketTimeoutException
import java.net.ConnectException
import java.util.concurrent.TimeUnit

class MemoryTokens : TokenStore {
    private var value: String? = null
    @Synchronized override fun token() = value
    @Synchronized override fun save(token: String) { value = token }
    @Synchronized override fun clear() { value = null }
    @Synchronized override fun clearIfMatches(token: String) { if (value == token) value = null }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ApiContractTest {
    @get:Rule val instant = InstantTaskExecutorRule()
    private lateinit var server: MockWebServer
    private lateinit var api: TrampoApi
    private lateinit var session: MemoryTokens

    @Before fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        session = MemoryTokens()
        server = MockWebServer()
        server.start()
        api = Retrofit.Builder().baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().retryOnConnectionFailure(false)
                .callTimeout(2, TimeUnit.SECONDS).addInterceptor(AuthInterceptor(session)).build())
            .addConverterFactory(GsonConverterFactory.create()).build().create(TrampoApi::class.java)
    }
    @After fun teardown() { server.shutdown(); Dispatchers.resetMain() }
    private fun json(body: String, code: Int = 200) {
        server.enqueue(MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body))
    }
    private val service = """{"id":4,"profissionalId":9,"subcategoriaId":2,"nome":"Reparo","descricao":null,"atendimento":"Domicilio","tipoPreco":"Fixo","precoBase":80.50,"ativo":true}"""
    private val user = """{"id":3,"nome":"Cliente","email":"cliente@example.invalid","tipo":"cliente"}"""

    @Test fun anonymousRequestOmitsAuthorization() = runBlocking {
        json("[]")
        assertTrue(api.servicos(2).isEmpty())
        val request = server.takeRequest()
        assertNull(request.getHeader("Authorization"))
        assertEquals("/api/v1/servicos?pagina=2&tamanho=20", request.path)
    }
    @Test fun loginSavesTokenBeforeFetchingMeAndDoesNotSendOldToken() = runBlocking {
        session.save("old")
        json("""{"accessToken":"test-token","tokenType":"Bearer","expiresAt":"2026-09-20T23:00:00Z"}""")
        json(user)
        assertEquals("Cliente", AuthRepository(api, session).login("cliente@example.invalid", "test-only").nome)
        val login = server.takeRequest()
        assertNull(login.getHeader("Authorization"))
        val body = Gson().fromJson(login.body.readUtf8(), Map::class.java)
        assertEquals(setOf("email", "senha"), body.keys)
        assertEquals("Bearer test-token", server.takeRequest().getHeader("Authorization"))
        assertEquals("test-token", session.token())
    }
    @Test fun unauthorizedClearsTokenButForbiddenDoesNot() = runBlocking {
        session.save("token")
        json("{}", 403)
        try { api.me(); fail() } catch (_: HttpException) { }
        assertEquals("token", session.token())
        json("{}", 401)
        try { api.me(); fail() } catch (_: HttpException) { }
        assertNull(session.token())
    }
    @Test fun lateUnauthorizedCannotEraseNewSession() {
        session.save("new-token")
        session.clearIfMatches("old-token")
        assertEquals("new-token", session.token())
    }
    @Test fun logoutClearsOnlyLocalSession() {
        session.save("token")
        AuthRepository(api, session).logout()
        assertNull(session.token())
        assertEquals(0, server.requestCount)
    }
    @Test fun nonClientProfileIsRejectedAndCleared() = runBlocking {
        session.save("token")
        json(user.replace("\"cliente\"}", "\"profissional\"}"))
        try { AuthRepository(api, session).me(); fail() } catch (_: PerfilNaoSuportado) { }
        assertNull(session.token())
    }
    @Test fun serviceNullableFieldsAndDecimalFollowContract() = runBlocking {
        json(service)
        val result = api.servico(4)
        assertNull(result.descricao)
        assertEquals("80.50", result.precoBase.toString())
        assertEquals("Domicilio", result.atendimento)
    }
    @Test fun agendaUsesIsoDateAndExactServerTimes() = runBlocking {
        json("""{"servicoId":4,"data":"2026-10-01","horarios":["09:00:00","14:30:00"]}""")
        val result = api.agenda(4, "2026-10-01")
        assertEquals(listOf("09:00:00", "14:30:00"), result.horarios)
        assertEquals("/api/v1/servicos/4/agenda?data=2026-10-01", server.takeRequest().path)
    }
    @Test fun bookingRequestDoesNotSendFinancialOwnershipOrStatusFields() = runBlocking {
        json("""{"id":12}""", 201)
        val address = EnderecoRequest("01001000", "Praça da Sé", "10", null, "Sé", "São Paulo", "SP")
        assertEquals(12, api.criar(AgendamentoRequest(4, "2026-10-01", "09:00:00", "Teste", address)).id)
        val request = server.takeRequest()
        val body = Gson().fromJson(request.body.readUtf8(), Map::class.java)
        assertEquals(setOf("servicoId", "data", "hora", "descricao", "endereco"), body.keys)
        assertEquals(setOf("cep", "logradouro", "numero", "bairro", "cidade", "uf"), (body["endereco"] as Map<*, *>).keys)
        assertEquals("POST", request.method)
    }
    @Test fun onlineRequestDoesNotSerializeAddressOrCoordinates() = runBlocking {
        json("""{"id":12}""", 201)
        api.criar(AgendamentoRequest(4, "2026-10-01", "09:00:00", null, null))
        val body = Gson().fromJson(server.takeRequest().body.readUtf8(), Map::class.java)
        assertEquals(setOf("servicoId", "data", "hora"), body.keys)
    }
    @Test fun cancelUsesPostAndAcceptsNoContent() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(204))
        api.cancelar(12)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/agendamentos/12/cancelar", request.path)
    }
    @Test fun cepGoesThroughTrampoAndRemovesFormatting() = runBlocking {
        json("""{"cep":"01001000","logradouro":"Praça da Sé","bairro":"Sé","cidade":"São Paulo","uf":"SP"}""")
        assertEquals("SP", EnderecoRepository(api).cep("01001-000").uf)
        assertEquals("/api/v1/enderecos/cep/01001000", server.takeRequest().path)
    }
    @Test fun errorsAreMappedByStatusWithoutDisplayingBody() = runBlocking {
        val mapping = mapOf(400 to R.string.error_validation, 401 to R.string.error_session,
            403 to R.string.error_forbidden, 404 to R.string.error_not_found,
            409 to R.string.error_conflict, 429 to R.string.error_rate_limit, 500 to R.string.error_server)
        for ((status, resource) in mapping) {
            json("""{"erro":{"mensagem":"SqlException secret"}}""", status)
            try { api.me(); fail() }
            catch (ex: HttpException) { assertEquals(resource, failureOf(ex).message) }
        }
    }
    @Test fun connectionAndTimeoutAreNotBadPassword() {
        assertEquals(R.string.error_connection, failureOf(ConnectException(), login = true).message)
        assertEquals(R.string.error_timeout, failureOf(SocketTimeoutException(), login = true).message)
        assertTrue(failureOf(SocketTimeoutException(), mutation = true).uncertain)
    }
    @Test fun cancellationIsNeverSwallowed() {
        try { failureOf(CancellationException()); fail() } catch (_: CancellationException) { }
    }
    @Test fun emptyCatalogueDisablesMorePages() = runBlocking {
        json("[]")
        val vm = ServicosViewModel(ServicoRepository(api))
        vm.load()
        withTimeout(3000) { while (vm.state.value is UiState.Loading) delay(10) }
        assertTrue(vm.state.value is UiState.Success)
        assertFalse(vm.hasMore)
        vm.load()
        assertEquals(1, server.requestCount)
    }
    @Test fun duplicateCreateTapSendsOnlyOneRequest() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(201).setBody("""{"id":12}""").setBodyDelay(100, TimeUnit.MILLISECONDS))
        val vm = NovoAgendamentoViewModel(ServicoRepository(api), AgendamentoRepository(api), EnderecoRepository(api))
        vm.review(AgendamentoRequest(4, "2026-10-01", "09:00:00", null, null), "Resumo")
        vm.create()
        vm.create()
        withTimeout(3000) { while (vm.creation.value is UiState.Loading) delay(10) }
        assertTrue(vm.creation.value is UiState.Success)
        vm.create()
        assertEquals(1, server.requestCount)
    }
    @Test fun uncertainCreateCannotBeRetriedByAnotherTap() = runBlocking {
        json("{}", 500)
        val vm = NovoAgendamentoViewModel(ServicoRepository(api), AgendamentoRepository(api), EnderecoRepository(api))
        vm.review(AgendamentoRequest(4, "2026-10-01", "09:00:00", null, null), "Resumo")
        vm.create()
        withTimeout(3000) { while (vm.creation.value is UiState.Loading) delay(10) }
        assertTrue((vm.creation.value as UiState.Error).failure.uncertain)
        vm.create()
        assertEquals(1, server.requestCount)
    }

    @Test fun registerUsesAnonymousRouteAndClientFieldsOnly() = runBlocking {
        session.save("expired")
        json("""{"accessToken":"new-token","tokenType":"Bearer","expiresAt":"2026-10-01T00:00:00Z"}""", 201)
        json(user)
        val result = AuthRepository(api,session).register(CadastroRequest("Cliente","cliente@example.invalid","testing-123","11999999999"))
        assertEquals("Cliente",result.nome)
        val request = server.takeRequest()
        assertEquals("/api/v1/auth/register",request.path)
        assertNull(request.getHeader("Authorization"))
        assertEquals(setOf("nome","email","senha","telefone"),Gson().fromJson(request.body.readUtf8(),Map::class.java).keys)
        assertEquals("Bearer new-token",server.takeRequest().getHeader("Authorization"))
    }
    @Test fun profileWritesOnlyOwnEditableFields() = runBlocking {
        session.save("token")
        server.enqueue(MockResponse().setResponseCode(204))
        ContaRepository(api).salvar(PerfilRequest("Novo nome","11999999999"))
        val request=server.takeRequest()
        assertEquals("PUT",request.method)
        assertEquals("/api/v1/perfil",request.path)
        assertEquals(setOf("nome","telefone"),Gson().fromJson(request.body.readUtf8(),Map::class.java).keys)
        assertEquals("Bearer token",request.getHeader("Authorization"))
    }
    @Test fun passwordRequestNeverSendsIdentityOrHash() = runBlocking {
        session.save("token")
        server.enqueue(MockResponse().setResponseCode(204))
        ContaRepository(api).senha(SenhaRequest("current-123","future-123","future-123"))
        val request=server.takeRequest()
        assertEquals("PUT",request.method)
        assertEquals("/api/v1/perfil/senha",request.path)
        assertEquals(setOf("senhaAtual","novaSenha","confirmarSenha"),Gson().fromJson(request.body.readUtf8(),Map::class.java).keys)
    }
    @Test fun discoveryKeepsServerPaginationAndPublicDetails() = runBlocking {
        json("[$service]")
        val page=ServicoRepository(api).listar(2,"Reparo","Domicilio")
        assertEquals(1,page.size)
        assertEquals("/api/v1/servicos?pagina=2&tamanho=20&busca=Reparo&atendimento=Domicilio",server.takeRequest().path)
    }
    @Test fun reviewAndNotificationUseBookingAndAuthenticatedRoutes() = runBlocking {
        session.save("token")
        json("""{"podeAvaliar":true,"jaAvaliado":false}""")
        assertTrue(AgendamentoRepository(api).estadoAvaliacao(9).podeAvaliar)
        assertEquals("/api/v1/agendamentos/9/avaliacao",server.takeRequest().path)
        server.enqueue(MockResponse().setResponseCode(204))
        AgendamentoRepository(api).avaliar(9,AvaliacaoRequest(5,"Bom atendimento"))
        val review=server.takeRequest()
        assertEquals("POST",review.method)
        assertEquals(setOf("nota","comentario"),Gson().fromJson(review.body.readUtf8(),Map::class.java).keys)
        json("[]")
        assertTrue(NotificacaoRepository(api).listar(1).isEmpty())
        assertEquals("/api/v1/notificacoes?pagina=1&tamanho=20",server.takeRequest().path)
        server.enqueue(MockResponse().setResponseCode(204))
        NotificacaoRepository(api).ler(7)
        assertEquals("/api/v1/notificacoes/7/ler",server.takeRequest().path)
    }
    @Test fun supportSendsOnlyTicketFields() = runBlocking {
        session.save("token")
        server.enqueue(MockResponse().setResponseCode(204))
        ContaRepository(api).suporte(SuporteRequest("Dúvida","Assunto","Mensagem"))
        val request=server.takeRequest()
        assertEquals("/api/v1/suporte",request.path)
        assertEquals(setOf("tipo","assunto","mensagem"),Gson().fromJson(request.body.readUtf8(),Map::class.java).keys)
    }
}