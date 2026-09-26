package com.example.trampo.ui

import androidx.lifecycle.*
import com.example.trampo.R
import com.example.trampo.data.repository.PerfilNaoSuportado
import com.example.trampo.data.repository.RespostaInvalida
import com.google.gson.JsonParseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val value: T) : UiState<T>
    data class Error(val failure: UiFailure) : UiState<Nothing>
}
data class UiFailure(val message: Int, val status: Int? = null, val uncertain: Boolean = false)

fun failureOf(error: Exception, login: Boolean = false, mutation: Boolean = false): UiFailure {
    if (error is CancellationException) throw error
    val status = (error as? HttpException)?.code()
    val message = when {
        error is PerfilNaoSuportado -> R.string.client_only
        error is SocketTimeoutException -> R.string.error_timeout
        error is IOException -> R.string.error_connection
        error is JsonParseException || error is RespostaInvalida -> R.string.error_response
        else -> when (status) {
            400 -> R.string.error_validation
            401 -> if (login) R.string.error_credentials else R.string.error_session
            403 -> R.string.error_forbidden
            404 -> R.string.error_not_found
            409 -> R.string.error_conflict
            429 -> R.string.error_rate_limit
            in 500..599 -> R.string.error_server
            else -> R.string.error_response
        }
    }
    val uncertain = mutation && (status == null || status >= 500)
    return UiFailure(if (uncertain) R.string.error_uncertain else message, status, uncertain)
}

open class RequestViewModel : ViewModel() {
    protected fun <T> request(
        state: MutableLiveData<UiState<T>>,
        login: Boolean = false,
        mutation: Boolean = false,
        action: suspend () -> T
    ) {
        if (state.value is UiState.Loading) return
        state.value = UiState.Loading
        viewModelScope.launch {
            try { state.value = UiState.Success(action()) }
            catch (ex: Exception) { state.value = UiState.Error(failureOf(ex, login, mutation)) }
        }
    }
}

class SimpleFactory<T : ViewModel>(private val create: () -> T) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <V : ViewModel> create(modelClass: Class<V>): V = create() as V
}