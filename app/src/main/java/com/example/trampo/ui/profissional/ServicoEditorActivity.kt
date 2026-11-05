package com.example.trampo.ui.profissional

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.lifecycle.ViewModelProvider
import com.example.trampo.R
import com.example.trampo.data.model.*
import com.example.trampo.ui.*

class ServicoEditorActivity : BaseActivity() {
    private lateinit var vm: ServicoEditorViewModel
    private val serviceId get() = intent.getIntExtra("id", 0)
    private var options: ServicoEditorData? = null
    private var current: ServicoProprioResponse? = null
    private var subcategories: List<SubcategoriaResponse> = emptyList()
    private val days = listOf(R.id.day_0, R.id.day_1, R.id.day_2, R.id.day_3, R.id.day_4, R.id.day_5, R.id.day_6)
    private val modalities = listOf("Local", "Domicilio", "Online")
    private val priceTypes = listOf("Fixo", "Combinar")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layout(R.layout.activity_servico_editor)
        vm = ViewModelProvider(this, SimpleFactory { ServicoEditorViewModel(app.profissional) })[ServicoEditorViewModel::class.java]
        if (serviceId > 0) findViewById<TextView>(R.id.page_title).setText(R.string.edit_service)
        vm.state.observe(this) { state ->
            render(state, R.id.retry)?.let { setup(it) }
        }
        vm.saved.observe(this) { state ->
            if (state !is UiState.Idle) render(state, R.id.save)
            if (state is UiState.Success) {
                Toast.makeText(this, R.string.service_saved, Toast.LENGTH_LONG).show()
                finish()
            }
            if (state is UiState.Error && state.failure.uncertain) findViewById<Button>(R.id.save).isEnabled = false
        }
        findViewById<Button>(R.id.save).setOnClickListener { save() }
        findViewById<Button>(R.id.retry).setOnClickListener { vm.saved.value = UiState.Idle; vm.load() }
        if (vm.state.value is UiState.Idle) vm.load()
    }

    private fun spinner(id: Int, labels: List<String>) = findViewById<Spinner>(id).apply {
        adapter = ArrayAdapter(this@ServicoEditorActivity, android.R.layout.simple_spinner_dropdown_item, labels)
    }
    private fun value(id: Int) = findViewById<EditText>(id).text.toString().trim()
    private fun text(id: Int, value: String?) = findViewById<EditText>(id).setText(value.orEmpty())
    private fun selection(id: Int, position: Int) = findViewById<Spinner>(id).setSelection(position.coerceAtLeast(0))

    private fun setup(data: ServicoEditorData) {
        options = data
        current = data.servicos.firstOrNull { it.id == serviceId }
        if (serviceId > 0 && current == null) {
            findViewById<TextView>(R.id.message).setText(R.string.error_not_found)
            findViewById<Button>(R.id.save).isEnabled = false
            return
        }
        val original = current
        text(R.id.name, original?.nome)
        text(R.id.description, original?.descricao)
        val categories = data.categorias
        spinner(R.id.category, categories.map { it.nome })
        fun updateSubcategories(index: Int) {
            subcategories = categories.getOrNull(index)?.subcategorias.orEmpty()
            spinner(R.id.subcategory, subcategories.map { it.nome })
        }
        findViewById<Spinner>(R.id.category).onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (original == null) updateSubcategories(position)
            }
        }
        if (original == null) updateSubcategories(0)
        else {
            findViewById<Spinner>(R.id.category).visibility = View.GONE
            findViewById<Spinner>(R.id.subcategory).visibility = View.GONE
            findViewById<TextView>(R.id.category_label).visibility = View.GONE
            findViewById<TextView>(R.id.subcategory_label).visibility = View.GONE
            findViewById<TextView>(R.id.category_fixed).apply {
                visibility = View.VISIBLE
                text = listOfNotNull(original.categoria, original.subcategoria).joinToString(" > ")
            }
        }
        spinner(R.id.modality, listOf(getString(R.string.local), getString(R.string.domicilio), getString(R.string.online)))
        selection(R.id.modality, modalities.indexOf(original?.atendimento))
        findViewById<Spinner>(R.id.modality).onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                findViewById<View>(R.id.local_section).visibility = if (modalities[position] == "Local") View.VISIBLE else View.GONE
                findViewById<View>(R.id.link_section).visibility = if (modalities[position] == "Online") View.VISIBLE else View.GONE
            }
        }
        spinner(R.id.local, data.locais.map { "${it.nome} · ${it.endereco}" })
        selection(R.id.local, data.locais.indexOfFirst { it.id == original?.localId })
        text(R.id.link_online, original?.linkOnline)
        spinner(R.id.price_type, listOf(getString(R.string.fixed_price), getString(R.string.price_agree)))
        selection(R.id.price_type, priceTypes.indexOf(original?.tipoPreco))
        findViewById<Spinner>(R.id.price_type).onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                findViewById<View>(R.id.price_section).visibility = if (priceTypes[position] == "Fixo") View.VISIBLE else View.GONE
            }
        }
        text(R.id.base_price, original?.precoBase?.toPlainString())
        val selectedDays = original?.diasSemana?.split(',')?.mapNotNull { it.trim().toIntOrNull() }.orEmpty()
        days.forEachIndexed { index, id -> findViewById<CheckBox>(id).isChecked = index in selectedDays }
        text(R.id.start_time, original?.horaInicio?.take(5) ?: "08:00")
        text(R.id.end_time, original?.horaFim?.take(5) ?: "17:00")
        findViewById<Button>(R.id.save).isEnabled = categories.isNotEmpty() || original != null
    }

    private fun save() {
        val data = options ?: return
        val original = current
        val name = value(R.id.name)
        if (name.isBlank()) { findViewById<EditText>(R.id.name).error = getString(R.string.required); return }
        val subcategoryId = original?.subcategoriaId ?: subcategories.getOrNull(
            findViewById<Spinner>(R.id.subcategory).selectedItemPosition)?.id ?: 0
        if (subcategoryId <= 0) { findViewById<TextView>(R.id.message).setText(R.string.select_subcategory); return }
        val modality = modalities[findViewById<Spinner>(R.id.modality).selectedItemPosition]
        val localId = if (modality == "Local") data.locais.getOrNull(findViewById<Spinner>(R.id.local).selectedItemPosition)?.id else null
        if (modality == "Local" && localId == null) {
            findViewById<TextView>(R.id.message).setText(R.string.need_local); return
        }
        val priceType = priceTypes[findViewById<Spinner>(R.id.price_type).selectedItemPosition]
        val price = if (priceType == "Fixo") value(R.id.base_price).replace(',', '.').toBigDecimalOrNull() else null
        if (priceType == "Fixo" && (price == null || price <= java.math.BigDecimal.ZERO)) {
            findViewById<EditText>(R.id.base_price).error = getString(R.string.error_validation); return
        }
        val selectedDays = days.mapIndexedNotNull { index, id -> index.takeIf { findViewById<CheckBox>(id).isChecked } }
        if (selectedDays.isEmpty()) { findViewById<TextView>(R.id.message).setText(R.string.select_day); return }
        val start = value(R.id.start_time)
        val end = value(R.id.end_time)
        val timePattern = Regex("^([01]?[0-9]|2[0-3]):[0-5][0-9]$")
        if (!timePattern.matches(start) || !timePattern.matches(end) || start == end) {
            findViewById<TextView>(R.id.message).setText(R.string.invalid_hours); return
        }
        vm.save(serviceId, ServicoRequest(subcategoryId, name, value(R.id.description), modality,
            localId, value(R.id.link_online).takeIf { modality == "Online" }, priceType, price,
            selectedDays.joinToString(","), "$start:00", "$end:00"))
    }
}
