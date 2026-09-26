package com.example.trampo.ui.agendamentos
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.lifecycle.ViewModelProvider
import com.example.trampo.R
import com.example.trampo.data.model.*
import com.example.trampo.ui.*
import java.util.Calendar
import java.util.Locale

class NovoAgendamentoActivity : BaseActivity() {
    private lateinit var vm: NovoAgendamentoViewModel
    private val fieldIds = listOf(R.id.cep, R.id.street, R.id.house_number, R.id.complement, R.id.district, R.id.city, R.id.uf, R.id.notes)
    private fun field(id: Int) = findViewById<EditText>(id)
    private fun value(id: Int) = field(id).text.toString().trim()
    private fun showReview() {
        val reviewing = vm.reviewed != null
        findViewById<View>(R.id.edit_form).visibility = if (reviewing) View.GONE else View.VISIBLE
        findViewById<View>(R.id.review_form).visibility = if (reviewing) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.summary).text = vm.reviewedSummary
    }
    private fun updateBusy() {
        val creating = vm.creation.value is UiState.Loading
        val agendaBusy = vm.agenda.value is UiState.Loading
        val addressBusy = vm.address.value is UiState.Loading
        val blocked = (vm.creation.value as? UiState.Error)?.failure?.uncertain == true
        val ready = vm.service.value is UiState.Success
        findViewById<Button>(R.id.date).isEnabled = ready && !creating && !agendaBusy && !blocked
        findViewById<Button>(R.id.review).isEnabled = ready && !creating && !agendaBusy && !addressBusy && !blocked
        findViewById<Button>(R.id.confirm).isEnabled = !creating && !blocked
        findViewById<Button>(R.id.edit).isEnabled = !creating && !blocked
        findViewById<Button>(R.id.lookup).isEnabled = !creating && !addressBusy && !blocked
        findViewById<Button>(R.id.retry).isEnabled = !creating && !agendaBusy && !blocked
        findViewById<Button>(R.id.back).isEnabled = !creating
        findViewById<Button>(R.id.bookings).isEnabled = !creating
        findViewById<Spinner>(R.id.time).isEnabled = !creating && !agendaBusy && !blocked
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layout(R.layout.activity_novo_agendamento)
        findViewById<Spinner>(R.id.time).adapter = ArrayAdapter(this,
            android.R.layout.simple_spinner_dropdown_item, listOf(getString(R.string.choose_date_first)))
        vm = ViewModelProvider(this, SimpleFactory { NovoAgendamentoViewModel(app.servicos, app.agendamentos, app.enderecos) })[NovoAgendamentoViewModel::class.java]
        vm.draft.forEach { (id, text) -> field(id).setText(text) }
        val id = intent.getIntExtra("id", 0)
        vm.service.observe(this) { state ->
            render(state)?.let {
                findViewById<TextView>(R.id.name).text = getString(R.string.two_lines, it.nome, it.presentation(this))
                findViewById<View>(R.id.address_form).visibility = if (it.atendimento == "Domicilio") View.VISIBLE else View.GONE
            }
            updateBusy()
        }
        vm.agenda.observe(this) { state ->
            render(state)?.let { agenda ->
                val spinner = findViewById<Spinner>(R.id.time)
                val times = agenda.horarios
                spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
                    listOf(getString(R.string.choose_time)) + times.map { it.take(5) })
                spinner.setSelection(times.indexOf(vm.selectedTime) + 1)
                spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>?) { vm.selectedTime = null }
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, itemId: Long) {
                        vm.selectedTime = times.getOrNull(position - 1)
                    }
                }
                if (times.isEmpty()) findViewById<TextView>(R.id.message).setText(R.string.no_times)
            }
            findViewById<TextView>(R.id.date_label).text = if (vm.selectedDate.isBlank()) "" else getString(R.string.date_selected, displayDate(vm.selectedDate))
            updateBusy()
        }
        vm.address.observe(this) { state ->
            val label = findViewById<TextView>(R.id.cep_message)
            when (state) {
                UiState.Loading -> label.setText(R.string.loading)
                is UiState.Error -> {
                    if (state.failure.status == 401) login(R.string.error_session)
                    else label.text = getString(R.string.two_lines, getString(state.failure.message), getString(R.string.address_manual))
                }
                is UiState.Success -> {
                    if (value(R.id.cep).filter(Char::isDigit) == vm.addressQuery) {
                        field(R.id.street).setText(state.value.logradouro)
                        field(R.id.district).setText(state.value.bairro)
                        field(R.id.city).setText(state.value.cidade)
                        field(R.id.uf).setText(state.value.uf)
                        label.setText(R.string.address_loaded)
                    }
                    vm.consumeAddress()
                }
                else -> Unit
            }
            updateBusy()
        }
        vm.creation.observe(this) { state ->
            if (state !is UiState.Idle) render(state)
            if (state is UiState.Success) {
                vm.consumeCreated()
                Toast.makeText(this, R.string.booking_created, Toast.LENGTH_LONG).show()
                startActivity(Intent(this, AgendamentosActivity::class.java).putExtra("created", true))
                finish()
            }
            if (state is UiState.Error && state.failure.status == 409) {
                vm.selectedTime = null
                vm.discardReview()
                showReview()
                findViewById<Spinner>(R.id.time).setSelection(0)
            }
            updateBusy()
        }
        findViewById<Button>(R.id.date).setOnClickListener {
            val current = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                vm.dates(id, String.format(Locale.ROOT, "%04d-%02d-%02d", year, month + 1, day))
                showReview()
            }, current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DAY_OF_MONTH)).show()
        }
        findViewById<Button>(R.id.lookup).setOnClickListener {
            if (value(R.id.cep).filter(Char::isDigit).length != 8) field(R.id.cep).error = getString(R.string.cep_invalid)
            else vm.cep(value(R.id.cep))
        }
        findViewById<Button>(R.id.review).setOnClickListener { review(id) }
        findViewById<Button>(R.id.confirm).setOnClickListener { vm.create() }
        findViewById<Button>(R.id.edit).setOnClickListener { vm.discardReview(); showReview() }
        findViewById<Button>(R.id.bookings).setOnClickListener { open(AgendamentosActivity::class.java); finish() }
        findViewById<Button>(R.id.retry).setOnClickListener {
            vm.load(id)
            if (vm.selectedDate.isNotEmpty()) vm.dates(id, vm.selectedDate)
            showReview()
        }
        showReview()
        if (vm.service.value is UiState.Idle) vm.load(id)
    }
    override fun onPause() {
        if (::vm.isInitialized) vm.draft = fieldIds.associateWith { value(it) }
        super.onPause()
    }
    private fun review(id: Int) {
        val service = (vm.service.value as? UiState.Success)?.value ?: return
        val agenda = (vm.agenda.value as? UiState.Success)?.value
        val time = vm.selectedTime
        if (time == null || agenda?.data != vm.selectedDate || !agenda.horarios.contains(time)) {
            findViewById<TextView>(R.id.message).setText(R.string.select_date_time)
            return
        }
        var address: EnderecoRequest? = null
        if (service.atendimento == "Domicilio") {
            val required = listOf(R.id.cep, R.id.street, R.id.house_number, R.id.district, R.id.city, R.id.uf)
            val missing = required.filter { value(it).isBlank() }
            missing.forEach { field(it).error = getString(R.string.required) }
            if (missing.isNotEmpty()) { field(missing.first()).requestFocus(); return }
            address = EnderecoRequest(value(R.id.cep), value(R.id.street), value(R.id.house_number),
                value(R.id.complement).ifBlank { null }, value(R.id.district), value(R.id.city), value(R.id.uf).uppercase(Locale.ROOT))
        }
        val request = AgendamentoRequest(id, vm.selectedDate, time, value(R.id.notes).ifBlank { null }, address)
        val addressText = address?.let {
            listOfNotNull(it.logradouro + ", " + it.numero, it.complemento, it.bairro, it.cidade + "/" + it.uf, it.cep).joinToString("\n")
        } ?: getString(R.string.no_address_needed)
        vm.review(request, getString(R.string.booking_summary, service.nome, service.presentation(this), displayDate(request.data), time.take(5), addressText))
        showReview()
    }
}