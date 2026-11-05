package com.example.trampo.ui.profissional

import android.os.Bundle
import android.widget.*
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.example.trampo.R
import com.example.trampo.data.model.AvaliacoesPublicasResponse
import com.example.trampo.data.repository.ProfissionalRepository
import com.example.trampo.ui.*
import java.util.Locale

class ProfissionalAvaliacoesViewModel(private val repository: ProfissionalRepository) : RequestViewModel() {
    val state = MutableLiveData<UiState<AvaliacoesPublicasResponse>>(UiState.Idle)
    fun load() = request(state) { repository.avaliacoes() }
}

class ProfissionalAvaliacoesActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layout(R.layout.activity_profissional_avaliacoes)
        val vm = ViewModelProvider(this, SimpleFactory { ProfissionalAvaliacoesViewModel(app.profissional) })[ProfissionalAvaliacoesViewModel::class.java]
        vm.state.observe(this) { state ->
            render(state, R.id.retry)?.let { result ->
                val summary = findViewById<TextView>(R.id.summary)
                val list = findViewById<LinearLayout>(R.id.reviews_list)
                list.removeAllViews()
                summary.text = if (result.total == 0) getString(R.string.no_received_reviews)
                    else resources.getQuantityString(R.plurals.reviews_summary, result.total,
                        String.format(Locale.forLanguageTag("pt-BR"), "%.1f", result.media), result.total)
                result.avaliacoes.forEach { review ->
                    val card = layoutInflater.inflate(R.layout.item_avaliacao_profissional, list, false)
                    card.findViewById<TextView>(R.id.stars).apply {
                        text = getString(R.string.review_stars, "${"★".repeat(review.nota.coerceIn(0, 5))}${"☆".repeat((5 - review.nota).coerceIn(0, 5))}")
                        contentDescription = getString(R.string.rating_out_of_five, review.nota)
                    }
                    card.findViewById<TextView>(R.id.service_name).text = review.servicoNome.orEmpty()
                    card.findViewById<TextView>(R.id.comment).apply {
                        text = review.comentario.orEmpty()
                        visibility = if (review.comentario.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE
                    }
                    card.findViewById<TextView>(R.id.customer).text = review.clienteNome
                    card.findViewById<TextView>(R.id.date).text = displayDate(review.data.take(10))
                    list.addView(card)
                }
            }
        }
        findViewById<Button>(R.id.retry).setOnClickListener { vm.load() }
        if (vm.state.value is UiState.Idle) vm.load()
    }
}
