package com.example.trampo.ui.conta

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.lifecycle.ViewModelProvider
import com.example.trampo.R
import com.example.trampo.data.model.*
import com.example.trampo.ui.*
import com.example.trampo.ui.home.HomeActivity

class CadastroActivity : BaseActivity() {
    override val requiresSession = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); layout(R.layout.activity_cadastro)
        val vm = ViewModelProvider(this, SimpleFactory { CadastroViewModel(app.auth) })[CadastroViewModel::class.java]
        val name = findViewById<EditText>(R.id.full_name)
        val email = findViewById<EditText>(R.id.email)
        val phone = findViewById<EditText>(R.id.phone)
        val password = findViewById<EditText>(R.id.password)
        vm.state.observe(this) { state ->
            render(state, R.id.submit)?.let { password.text.clear(); route(HomeActivity::class.java) }
            listOf(name,email,phone,password).forEach { it.isEnabled = state !is UiState.Loading }
            if (state is UiState.Error && state.failure.uncertain) {
                findViewById<Button>(R.id.submit).isEnabled = false
                findViewById<TextView>(R.id.message).setText(R.string.register_uncertain)
            }
        }
        findViewById<Button>(R.id.submit).setOnClickListener {
            val n = name.text.toString().trim(); val mail = email.text.toString().trim(); val secret = password.text.toString()
            when {
                n.isBlank() -> name.error = getString(R.string.required)
                !android.util.Patterns.EMAIL_ADDRESS.matcher(mail).matches() -> email.error = getString(R.string.error_validation)
                secret.isBlank() || secret.length !in 8..1024 -> password.error = getString(R.string.password_hint)
                else -> { vm.enviar(CadastroRequest(n,mail,secret,phone.text.toString().trim())); password.text.clear() }
            }
        }
    }
}
class PerfilActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); layout(R.layout.activity_perfil)
        val vm = ViewModelProvider(this, SimpleFactory { PerfilViewModel(app.conta) })[PerfilViewModel::class.java]
        val name = findViewById<EditText>(R.id.full_name); val phone = findViewById<EditText>(R.id.phone)
        vm.state.observe(this) { state ->
            render(state, R.id.retry)?.let {
                name.setText(it.nome); phone.setText(it.telefone)
                findViewById<TextView>(R.id.profile_email).text = it.email
            }
            findViewById<Button>(R.id.submit).isEnabled = state is UiState.Success
        }
        vm.saved.observe(this) { state ->
            if (state !is UiState.Idle) render(state, R.id.submit, R.id.retry)
            name.isEnabled = state !is UiState.Loading; phone.isEnabled = state !is UiState.Loading
            if (state is UiState.Success) findViewById<TextView>(R.id.message).setText(R.string.profile_saved)
            if (state is UiState.Error && state.failure.uncertain) {
                findViewById<Button>(R.id.submit).isEnabled = false
                findViewById<TextView>(R.id.message).setText(R.string.mutation_uncertain)
            }
        }
        findViewById<Button>(R.id.submit).setOnClickListener {
            if (name.text.isBlank()) name.error = getString(R.string.required)
            else vm.salvar(PerfilRequest(name.text.toString().trim(),phone.text.toString().trim()))
        }
        findViewById<Button>(R.id.change_password).setOnClickListener { open(SenhaActivity::class.java) }
        findViewById<Button>(R.id.retry).setOnClickListener { vm.saved.value = UiState.Idle; vm.load() }
        if (vm.state.value is UiState.Idle) vm.load()
    }
}
class SenhaActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); layout(R.layout.activity_senha)
        val vm = ViewModelProvider(this, SimpleFactory { SenhaViewModel(app.conta) })[SenhaViewModel::class.java]
        val current = findViewById<EditText>(R.id.current_password)
        val next = findViewById<EditText>(R.id.new_password)
        val confirm = findViewById<EditText>(R.id.confirm_password)
        vm.state.observe(this) { state ->
            render(state, R.id.submit)
            listOf(current,next,confirm).forEach { it.isEnabled = state !is UiState.Loading }
            if (state is UiState.Success) {
                findViewById<TextView>(R.id.message).setText(R.string.password_changed)
                findViewById<Button>(R.id.submit).isEnabled = false
            }
            if (state is UiState.Error && state.failure.uncertain) {
                findViewById<Button>(R.id.submit).isEnabled = false
                findViewById<TextView>(R.id.message).setText(R.string.mutation_uncertain)
            }
        }
        findViewById<Button>(R.id.submit).setOnClickListener {
            when {
                current.text.isBlank() -> current.error = getString(R.string.required)
                next.text.isBlank() || next.text.length !in 8..1024 || next.text.toString() != confirm.text.toString() ->
                    confirm.error = getString(R.string.password_mismatch)
                else -> {
                    vm.enviar(SenhaRequest(current.text.toString(),next.text.toString(),confirm.text.toString()))
                    listOf(current,next,confirm).forEach { it.text.clear() }
                }
            }
        }
    }
}
class SuporteActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); layout(R.layout.activity_suporte)
        val vm = ViewModelProvider(this, SimpleFactory { SuporteViewModel(app.conta) })[SuporteViewModel::class.java]
        val subject = findViewById<EditText>(R.id.subject); val message = findViewById<EditText>(R.id.support_message)
        vm.state.observe(this) { state ->
            render(state, R.id.submit)
            subject.isEnabled = state !is UiState.Loading; message.isEnabled = state !is UiState.Loading
            if (state is UiState.Success) {
                subject.text.clear(); message.text.clear()
                findViewById<TextView>(R.id.message).setText(R.string.support_sent)
                findViewById<Button>(R.id.submit).isEnabled = false
            }
            if (state is UiState.Error && state.failure.uncertain) {
                findViewById<Button>(R.id.submit).isEnabled = false
                findViewById<TextView>(R.id.message).setText(R.string.mutation_uncertain)
            }
        }
        findViewById<Button>(R.id.submit).setOnClickListener {
            when {
                subject.text.isBlank() -> subject.error = getString(R.string.required)
                message.text.isBlank() -> message.error = getString(R.string.required)
                else -> vm.enviar(SuporteRequest("Dúvida",subject.text.toString().trim(),message.text.toString().trim()))
            }
        }
    }
}
