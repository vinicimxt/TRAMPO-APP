package com.example.trampo.data.api

import com.example.trampo.data.model.*
import retrofit2.http.*
import java.math.BigDecimal

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
    @GET("api/v1/servicos/{id}/avaliacoes") suspend fun avaliacoesPublicas(@Path("id") id: Int): AvaliacoesPublicasResponse
    @GET("api/v1/profissionais/me/avaliacoes") suspend fun avaliacoesRecebidas(): AvaliacoesPublicasResponse
    @GET("api/v1/servicos/me") suspend fun servicosProprios(): List<ServicoProprioResponse>
    @GET("api/v1/categorias") suspend fun categorias(): List<CategoriaResponse>
    @POST("api/v1/servicos") suspend fun criarServico(@Body request: ServicoRequest): RecursoCriadoResponse
    @PUT("api/v1/servicos/{id}") suspend fun editarServico(@Path("id") id: Int, @Body request: ServicoRequest)
    @DELETE("api/v1/servicos/{id}") suspend fun removerServico(@Path("id") id: Int): RemocaoResponse
    @GET("api/v1/locais") suspend fun locais(): List<LocalResponse>
    @GET("api/v1/locais/{id}") suspend fun local(@Path("id") id: Int): LocalResponse
    @POST("api/v1/locais") suspend fun criarLocal(@Body request: LocalRequest): RecursoCriadoResponse
    @PUT("api/v1/locais/{id}") suspend fun editarLocal(@Path("id") id: Int, @Body request: LocalRequest)
    @DELETE("api/v1/locais/{id}") suspend fun excluirLocal(@Path("id") id: Int)
    @GET("api/v1/servicos/{id}/agenda") suspend fun agenda(@Path("id") id: Int, @Query("data") data: String): AgendaResponse
    @GET("api/v1/agendamentos") suspend fun agendamentos(@Query("visao") visao: String = "meus"): List<AgendamentoResponse>
    @GET("api/v1/agendamentos/{id}") suspend fun agendamento(@Path("id") id: Int): AgendamentoResponse
    @POST("api/v1/agendamentos") suspend fun criar(@Body request: AgendamentoRequest): RecursoCriadoResponse
    @POST("api/v1/agendamentos/{id}/cancelar") suspend fun cancelar(@Path("id") id: Int)
    @POST("api/v1/agendamentos/{id}/confirmar") suspend fun confirmar(@Path("id") id: Int)
    @POST("api/v1/agendamentos/{id}/recusar") suspend fun recusar(@Path("id") id: Int)
    @POST("api/v1/agendamentos/{id}/finalizar") suspend fun finalizar(@Path("id") id: Int, @Body request: FinalizarRequest)
    @GET("api/v1/enderecos/cep/{cep}") suspend fun cep(@Path("cep") cep: String): EnderecoResponse
}
data class FinalizarRequest(val valorFinal: BigDecimal)
