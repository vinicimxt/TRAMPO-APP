package com.example.trampo.ui.profissional

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.example.trampo.R
import com.example.trampo.data.model.*
import com.example.trampo.ui.*
import java.text.NumberFormat
import java.util.Locale

class ProfissionalServicosActivity : BaseActivity() {
    private lateinit var vm: ProfissionalServicosViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layout(R.layout.activity_profissional_servicos)
        if (intent.getBooleanExtra("availability", false)) {
            findViewById<TextView>(R.id.page_title).setText(R.string.availability)
            findViewById<TextView>(R.id.page_subtitle).setText(R.string.availability_hint)
        }
        vm = ViewModelProvider(this, SimpleFactory { ProfissionalServicosViewModel(app.profissional) })[ProfissionalServicosViewModel::class.java]
        vm.state.observe(this) { state ->
            render(state, R.id.retry)?.let { servicos ->
                val list = findViewById<LinearLayout>(R.id.services_list)
                list.removeAllViews()
                if (servicos.isEmpty()) findViewById<TextView>(R.id.message).setText(R.string.empty_own_services)
                servicos.forEach { service ->
                    val card = layoutInflater.inflate(R.layout.item_servico_proprio, list, false)
                    card.findViewById<TextView>(R.id.name).text = service.nome
                    card.findViewById<TextView>(R.id.category).text = listOfNotNull(service.categoria, service.subcategoria).joinToString(" > ")
                    card.findViewById<TextView>(R.id.price).text = if (service.tipoPreco == "Fixo" && service.precoBase != null)
                        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(service.precoBase)
                        else getString(R.string.price_agree)
                    card.findViewById<TextView>(R.id.modality).text = when (service.atendimento) {
                        "Local" -> getString(R.string.local); "Online" -> getString(R.string.online)
                        "Domicilio" -> getString(R.string.domicilio); else -> service.atendimento
                    }
                    card.findViewById<TextView>(R.id.status).text = getString(if (service.ativo) R.string.active_service else R.string.inactive_service)
                    val names = listOf(R.string.sunday, R.string.monday, R.string.tuesday, R.string.wednesday,
                        R.string.thursday, R.string.friday, R.string.saturday)
                    val days = service.diasSemana.split(',').mapNotNull { it.trim().toIntOrNull() }
                        .filter { it in 0..6 }.map { getString(names[it]) }
                    card.findViewById<TextView>(R.id.schedule).text = if (days.isEmpty()) getString(R.string.no_availability)
                        else "${days.joinToString(", ")} · ${service.horaInicio?.take(5).orEmpty()}–${service.horaFim?.take(5).orEmpty()}"
                    card.findViewById<Button>(R.id.edit).setOnClickListener { open(ServicoEditorActivity::class.java, service.id) }
                    card.findViewById<Button>(R.id.remove).setOnClickListener {
                        AlertDialog.Builder(this).setTitle(R.string.remove_service)
                            .setMessage(R.string.remove_service_question).setNegativeButton(R.string.back, null)
                            .setPositiveButton(R.string.remove_service) { _, _ -> vm.remove(service.id) }.show()
                    }
                    list.addView(card)
                }
            }
        }
        vm.removal.observe(this) { state ->
            if (state !is UiState.Idle) render(state, R.id.new_service)
            if (state is UiState.Success) findViewById<TextView>(R.id.message).setText(R.string.service_removed)
            if (state is UiState.Error) findViewById<Button>(R.id.new_service).isEnabled = false
        }
        findViewById<Button>(R.id.new_service).setOnClickListener { open(ServicoEditorActivity::class.java) }
        findViewById<Button>(R.id.retry).setOnClickListener {
            vm.removal.value = UiState.Idle
            findViewById<Button>(R.id.new_service).isEnabled = true
            vm.load()
        }
    }
    override fun onResume() {
        super.onResume()
        if (::vm.isInitialized && !app.session.token().isNullOrBlank()) vm.load()
    }
}
