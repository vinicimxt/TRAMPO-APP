package com.example.trampo.data.api

import com.example.trampo.data.model.*
import retrofit2.http.*

interface TrampoApi {
    @POST("api/v1/auth/register") suspend fun register(@Body request: CadastroRequest): LoginResponse
    @GET("api/v1/perfil") suspend fun perfil(): PerfilResponse
    @PUT("api/v1/perfil") suspend fun salvarPerfil(@Body request: PerfilRequest)
    @PUT("api/v1/perfil/senha") suspend fun senha(@Body request: SenhaRequest)
    @GET("api/v1/notificacoes") suspend fun notificacoes(@Query("pagina") pagina: Int, @Query("tamanho") tamanho: Int = 20): List<NotificacaoResponse>
    @POST("api/v1/notificacoes/{id}/ler") suspend fun ler(@Path("id") id: Int)
    @POST("api/v1/suporte") suspend fun suporte(@Body request: SuporteRequest)
    @POST("api/v1/agendamentos/{id}/confirmar-conclusao") suspend fun concluir(@Path("id") id: Int)
    @GET("api/v1/agendamentos/{id}/avaliacao") suspend fun estadoAvaliacao(@Path("id") id: Int): EstadoAvaliacaoResponse
    @POST("api/v1/agendamentos/{id}/avaliacao") suspend fun avaliar(@Path("id") id: Int, @Body request: AvaliacaoRequest)

    @POST("api/v1/auth/login") suspend fun login(@Body request: LoginRequest): LoginResponse
    @GET("api/v1/auth/me") suspend fun me(): UsuarioResponse
    @GET("api/v1/servicos") suspend fun servicos(@Query("pagina") pagina: Int, @Query("tamanho") tamanho: Int = 20, @Query("busca") busca: String? = null, @Query("atendimento") atendimento: String? = null): List<ServicoResponse>
    @GET("api/v1/servicos/{id}") suspend fun servico(@Path("id") id: Int): ServicoResponse
    @GET("api/v1/servicos/{id}/agenda") suspend fun agenda(@Path("id") id: Int, @Query("data") data: String): AgendaResponse
    @GET("api/v1/agendamentos") suspend fun agendamentos(): List<AgendamentoResponse>
    @GET("api/v1/agendamentos/{id}") suspend fun agendamento(@Path("id") id: Int): AgendamentoResponse
    @POST("api/v1/agendamentos") suspend fun criar(@Body request: AgendamentoRequest): RecursoCriadoResponse
    @POST("api/v1/agendamentos/{id}/cancelar") suspend fun cancelar(@Path("id") id: Int)
    @GET("api/v1/enderecos/cep/{cep}") suspend fun cep(@Path("cep") cep: String): EnderecoResponse
}