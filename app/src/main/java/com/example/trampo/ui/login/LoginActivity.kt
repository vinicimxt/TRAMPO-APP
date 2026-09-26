package com.example.trampo.ui.login

import android.os.Bundle
import android.widget.*
import androidx.lifecycle.ViewModelProvider
import com.example.trampo.R
import com.example.trampo.ui.*
import com.example.trampo.ui.home.HomeActivity

class LoginActivity : BaseActivity() {
    override val requiresSession = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layout(R.layout.activity_login)
        val vm = ViewModelProvider(this, SimpleFactory { LoginViewModel(app.auth) })[LoginViewModel::class.java]
        findViewById<Button>(R.id.create_account).setOnClickListener { open(com.example.trampo.ui.conta.CadastroActivity::class.java) }
        val email = findViewById<EditText>(R.id.email)
        val password = findViewById<EditText>(R.id.password)
        vm.state.observe(this) { state ->
            render(state, R.id.submit)?.let { password.text.clear(); route(HomeActivity::class.java) }
            email.isEnabled = state !is UiState.Loading
            password.isEnabled = state !is UiState.Loading
        }
        intent.getIntExtra("message", 0).takeIf { it != 0 }?.let {
            findViewById<TextView>(R.id.message).setText(it)
            intent.removeExtra("message")
        }
        password.setOnEditorActionListener { _, action, _ ->
            if (action == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                findViewById<Button>(R.id.submit).let { if (it.isEnabled) it.performClick() }
                true
            } else false
        }
        findViewById<Button>(R.id.submit).setOnClickListener {
            val address = email.text.toString().trim()
            val secret = password.text.toString()
            if (address.isEmpty()) email.error = getString(R.string.required)
            else if (secret.isEmpty()) password.error = getString(R.string.required)
            else {
                vm.login(address, secret)
                password.text.clear()
            }
        }
    }
}