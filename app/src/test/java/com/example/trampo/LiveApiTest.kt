package com.example.trampo

import com.example.trampo.data.api.*
import com.example.trampo.data.model.*
import com.example.trampo.data.repository.*
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// Executado somente pelo runner Windows que cria e remove as próprias fixtures.
// O cliente Kotlin usa exclusivamente HTTP, inclusive neste teste.
class LiveApiTest {
    private fun requiredEnv(name: String): String = requireNotNull(System.getenv(name)) { "Variável de fixture ausente: $name" }
    @Test fun clienteReservaNasTresModalidadesPelaApiReal() = runBlocking {
        val endpoint = System.getenv("TRAMPO_LIVE_URL")
        assumeTrue("Execute Tests/Sprint6/Run.ps1 para integrar com SQL Server.", !endpoint.isNullOrBlank())
        val session = MemoryTokens()
        val api = Retrofit.Builder().baseUrl(endpoint!!)
            .client(OkHttpClient.Builder().addInterceptor(AuthInterceptor(session))
                .callTimeout(20, TimeUnit.SECONDS).retryOnConnectionFailure(false).build())
            .addConverterFactory(GsonConverterFactory.create()).build().create(TrampoApi::class.java)
        val auth = AuthRepository(api, session)
        val me = auth.login(requiredEnv("TRAMPO_LIVE_EMAIL"), requiredEnv("TRAMPO_LIVE_PASSWORD"))
        assertEquals("cliente", me.tipo)
        val ids = requiredEnv("TRAMPO_LIVE_SERVICES").split(",").map(String::toInt)
        val date = requiredEnv("TRAMPO_LIVE_DATE")
        val used = mutableSetOf<String>()
        for (id in ids) {
            val service = api.servico(id)
            val slots = api.agenda(id, date)
            val time = slots.horarios.first { it !in used }
            used.add(time)
            val address = if (service.atendimento == "Domicilio")
                EnderecoRequest("01001000", "Praça da Sé", "10", null, "Sé", "São Paulo", "SP") else null
            val created = api.criar(AgendamentoRequest(id, date, time, "Fixture Sprint 6 Kotlin", address))
            assertTrue(created.id > 0)
            val detail = api.agendamento(created.id)
            assertEquals("Pendente", detail.status)
            assertEquals(id, detail.servicoId)
            assertEquals(time, detail.hora)
            if (service.atendimento == "Online") assertNull(detail.enderecoAtendimento)
            else assertNotNull(detail.enderecoAtendimento)
            assertTrue(api.agendamentos().any { it.id == created.id })
            if (service.atendimento == "Online") {
                api.cancelar(created.id)
                assertEquals("CanceladoCliente", api.agendamento(created.id).status)
            }
        }
        assertEquals(3, api.agendamentos().size)
        auth.logout()
        assertFalse(auth.hasSession())
    }
}