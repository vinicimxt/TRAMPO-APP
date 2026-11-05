package com.example.trampo.ui.profissional

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.lifecycle.ViewModelProvider
import com.example.trampo.R
import com.example.trampo.data.model.*
import com.example.trampo.ui.*

class LocalEditorActivity : BaseActivity() {
    private lateinit var vm: LocalEditorViewModel
    private val localId get() = intent.getIntExtra("id", 0)
    private fun field(id: Int) = findViewById<EditText>(id)
    private fun value(id: Int) = field(id).text.toString().trim()
    private fun set(id: Int, text: String?) = field(id).setText(text.orEmpty())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layout(R.layout.activity_local_editor)
        findViewById<View>(R.id.address_form).visibility = View.VISIBLE
        if (localId > 0) findViewById<TextView>(R.id.page_title).setText(R.string.edit_location)
        vm = ViewModelProvider(this, SimpleFactory { LocalEditorViewModel(app.profissional, app.enderecos) })[LocalEditorViewModel::class.java]
        vm.state.observe(this) { state ->
            render(state, R.id.retry)
            if (state is UiState.Success && state.value != null) {
                val local = state.value
                set(R.id.name, local.nome)
                val address = local.dadosEndereco
                set(R.id.cep, address?.cep)
                set(R.id.street, address?.logradouro)
                set(R.id.house_number, address?.numero)
                set(R.id.complement, address?.complemento)
                set(R.id.district, address?.bairro)
                set(R.id.city, address?.cidade)
                set(R.id.uf, address?.uf)
                if (address == null) findViewById<TextView>(R.id.message).setText(R.string.legacy_location_address)
            }
        }
        vm.address.observe(this) { state ->
            val message = findViewById<TextView>(R.id.cep_message)
            when (state) {
                is UiState.Loading -> message.setText(R.string.loading)
                is UiState.Error -> {
                    if (state.failure.status == 401) login(R.string.error_session)
                    else message.setText(R.string.address_manual)
                }
                is UiState.Success -> {
                    if (value(R.id.cep).filter(Char::isDigit) == state.value.cep.filter(Char::isDigit)) {
                        set(R.id.street, state.value.logradouro)
                        set(R.id.district, state.value.bairro)
                        set(R.id.city, state.value.cidade)
                        set(R.id.uf, state.value.uf)
                        message.setText(R.string.address_loaded)
                    }
                }
                else -> Unit
            }
        }
        vm.saved.observe(this) { state ->
            if (state !is UiState.Idle) render(state, R.id.save, R.id.lookup)
            if (state is UiState.Error && state.failure.status == 409) findViewById<TextView>(R.id.message).setText(R.string.location_in_use)
            if (state is UiState.Success) {
                Toast.makeText(this, R.string.location_saved, Toast.LENGTH_LONG).show()
                finish()
            }
            if (state is UiState.Error && state.failure.uncertain) findViewById<Button>(R.id.save).isEnabled = false
        }
        findViewById<Button>(R.id.lookup).setOnClickListener {
            if (value(R.id.cep).filter(Char::isDigit).length != 8) field(R.id.cep).error = getString(R.string.cep_invalid)
            else vm.cep(value(R.id.cep))
        }
        findViewById<Button>(R.id.save).setOnClickListener { save() }
        findViewById<Button>(R.id.retry).setOnClickListener {
            vm.saved.value = UiState.Idle
            findViewById<Button>(R.id.save).isEnabled = true
            vm.load(localId)
        }
        if (vm.state.value is UiState.Idle) vm.load(localId)
    }

    private fun save() {
        if (value(R.id.name).isBlank()) { field(R.id.name).error = getString(R.string.required); return }
        val cep = value(R.id.cep).filter(Char::isDigit)
        if (cep.length != 8) { field(R.id.cep).error = getString(R.string.cep_invalid); return }
        val required = listOf(R.id.street, R.id.house_number, R.id.district, R.id.city, R.id.uf)
        val missing = required.firstOrNull { value(it).isBlank() }
        if (missing != null) { field(missing).error = getString(R.string.required); return }
        vm.save(localId, LocalRequest(value(R.id.name), EnderecoRequest(cep, value(R.id.street),
            value(R.id.house_number), value(R.id.complement).takeIf { it.isNotBlank() },
            value(R.id.district), value(R.id.city), value(R.id.uf).uppercase())))
    }
}
