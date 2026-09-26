package com.example.trampo.ui.conta

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.example.trampo.R
import com.example.trampo.data.model.*
import com.example.trampo.data.repository.AgendamentoRepository
import com.example.trampo.ui.*

class AvaliacaoViewModel(private val repository: AgendamentoRepository) : RequestViewModel() {
    val state = MutableLiveData<UiState<EstadoAvaliacaoResponse>>(UiState.Idle)
    val sent = MutableLiveData<UiState<Unit>>(UiState.Idle)
    fun load(id: Int) = request(state) { repository.estadoAvaliacao(id) }
    fun enviar(id: Int, dados: AvaliacaoRequest) {
        if ((state.value as? UiState.Success)?.value?.podeAvaliar != true || sent.value is UiState.Success ||
            (sent.value as? UiState.Error)?.failure?.uncertain == true) return
        request(sent, mutation = true) {
            repository.avaliar(id,dados)
            state.value = UiState.Success(EstadoAvaliacaoResponse(false,true))
        }
    }
}
class AvaliacaoActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); layout(R.layout.activity_avaliacao)
        val id = intent.getIntExtra("id",0)
        val vm = ViewModelProvider(this, SimpleFactory { AvaliacaoViewModel(app.agendamentos) })[AvaliacaoViewModel::class.java]
        val rating = findViewById<Spinner>(R.id.rating)
        rating.adapter = ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,listOf("1","2","3","4","5"))
        val comment = findViewById<EditText>(R.id.review_comment)
        vm.state.observe(this) { state ->
            render(state, R.id.retry)?.let {
                findViewById<TextView>(R.id.message).setText(if(it.jaAvaliado) R.string.review_done else if(!it.podeAvaliar) R.string.review_unavailable else R.string.rating_hint)
            }
            val allowed = (state as? UiState.Success)?.value?.podeAvaliar == true && vm.sent.value !is UiState.Loading &&
                (vm.sent.value as? UiState.Error)?.failure?.uncertain != true
            findViewById<Button>(R.id.submit).isEnabled = allowed
            rating.isEnabled = allowed; comment.isEnabled = allowed
        }
        vm.sent.observe(this) { state ->
            if (state !is UiState.Idle) render(state, R.id.retry)
            val allowed = (vm.state.value as? UiState.Success)?.value?.podeAvaliar == true && state !is UiState.Loading &&
                (state as? UiState.Error)?.failure?.uncertain != true && state !is UiState.Success
            findViewById<Button>(R.id.submit).isEnabled = allowed
            rating.isEnabled = allowed; comment.isEnabled = allowed
            if (state is UiState.Success) { comment.text.clear(); findViewById<TextView>(R.id.message).setText(R.string.review_sent) }
        }
        findViewById<Button>(R.id.submit).setOnClickListener {
            AlertDialog.Builder(this).setTitle(R.string.send_review).setMessage(R.string.review_question)
                .setNegativeButton(R.string.back,null).setPositiveButton(R.string.send_review) { _,_ ->
                    vm.enviar(id,AvaliacaoRequest(rating.selectedItemPosition+1,comment.text.toString().trim()))
                }.show()
        }
        findViewById<Button>(R.id.retry).setOnClickListener { vm.sent.value = UiState.Idle; vm.load(id) }
        if (vm.state.value is UiState.Idle) vm.load(id)
    }
}
