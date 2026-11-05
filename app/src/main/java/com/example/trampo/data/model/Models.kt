package com.example.trampo.data.model

import java.math.BigDecimal

data class LoginRequest(val email: String, val senha: String)
data class LoginResponse(val accessToken: String, val tokenType: String, val expiresAt: String)
data class UsuarioResponse(val id: Int, val nome: String, val email: String, val tipo: String)
data class ServicoResponse(
    val id: Int, val profissionalId: Int, val subcategoriaId: Int,
    val nome: String, val descricao: String?, val atendimento: String,
    val tipoPreco: String, val precoBase: BigDecimal?, val ativo: Boolean,
    val nomeProfissional: String? = null, val categoria: String? = null, val subcategoria: String? = null
)
data class AgendaResponse(val servicoId: Int, val data: String, val horarios: List<String>)
data class EnderecoResponse(val cep: String, val logradouro: String, val bairro: String, val cidade: String, val uf: String)
data class EnderecoRequest(
    val cep: String, val logradouro: String, val numero: String, val complemento: String?,
    val bairro: String, val cidade: String, val uf: String
)
data class AgendamentoRequest(
    val servicoId: Int, val data: String, val hora: String,
    val descricao: String?, val endereco: EnderecoRequest?
)
data class RecursoCriadoResponse(val id: Int)
data class EnderecoAtendimentoResponse(
    val cep: String?, val logradouro: String?, val numero: String?, val complemento: String?,
    val bairro: String?, val cidade: String?, val uf: String?,
    val enderecoFormatado: String, val estruturado: Boolean
)
data class AgendamentoResponse(
    val id: Int, val servicoId: Int, val profissionalId: Int, val data: String,
    val hora: String, val status: String, val descricao: String?,
    val enderecoCliente: String?, val valorFinal: BigDecimal?, val origemValorFinal: String,
    val enderecoAtendimento: EnderecoAtendimentoResponse?,
    val servicoNome: String? = null, val profissionalNome: String? = null,
    val clienteNome: String? = null, val atendimento: String? = null, val local: String? = null,
    val tipoPreco: String? = null, val precoBase: BigDecimal? = null
)
data class CadastroRequest(val nome: String, val email: String, val senha: String, val telefone: String?)
data class PerfilResponse(val nome: String, val email: String, val telefone: String?)
data class PerfilRequest(val nome: String, val telefone: String?)
data class SenhaRequest(val senhaAtual: String, val novaSenha: String, val confirmarSenha: String)
data class SuporteRequest(val tipo: String, val assunto: String, val mensagem: String)
data class NotificacaoResponse(val id: Int, val titulo: String, val mensagem: String, val dataCriacao: String, val lida: Boolean)
data class EstadoAvaliacaoResponse(val podeAvaliar: Boolean, val jaAvaliado: Boolean)
data class AvaliacaoRequest(val nota: Int, val comentario: String?)
data class AvaliacaoPublicaResponse(val nota: Int, val comentario: String?, val clienteNome: String, val data: String, val servicoNome: String? = null)
data class AvaliacoesPublicasResponse(val media: Double?, val total: Int, val avaliacoes: List<AvaliacaoPublicaResponse>)
data class SubcategoriaResponse(val id: Int, val nome: String)
data class CategoriaResponse(val id: Int, val nome: String, val subcategorias: List<SubcategoriaResponse>)
data class ServicoProprioResponse(
    val id: Int, val subcategoriaId: Int, val nome: String, val descricao: String?,
    val atendimento: String, val localId: Int?, val linkOnline: String?,
    val tipoPreco: String, val precoBase: BigDecimal?, val ativo: Boolean,
    val diasSemana: String, val horaInicio: String?, val horaFim: String?,
    val categoria: String?, val subcategoria: String?
)
data class ServicoRequest(
    val subcategoriaId: Int, val nome: String, val descricao: String?,
    val atendimento: String, val localId: Int?, val linkOnline: String?,
    val tipoPreco: String, val precoBase: BigDecimal?, val diasSemana: String,
    val horaInicio: String, val horaFim: String
)
data class RemocaoResponse(val id: Int, val resultado: String)
data class LocalResponse(val id: Int, val nome: String, val endereco: String, val dadosEndereco: EnderecoAtendimentoResponse?)
data class LocalRequest(val nome: String, val dadosEndereco: EnderecoRequest)
