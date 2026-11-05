package com.example.trampo

import android.os.Bundle
import android.widget.Button
import androidx.lifecycle.ViewModelProvider
import com.example.trampo.ui.*
import com.example.trampo.ui.home.*
import com.example.trampo.ui.profissional.ProfissionalHomeActivity

class MainActivity : BaseActivity() {
    override val requiresSession = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layout(R.layout.activity_main)
        val vm = ViewModelProvider(this, SimpleFactory { HomeViewModel(app.auth) })[HomeViewModel::class.java]
        if (!vm.hasSession()) { login(); return }
        vm.state.observe(this) { state ->
            render(state, R.id.retry)?.let { user ->
                route(if (user.tipo == "profissional") ProfissionalHomeActivity::class.java else HomeActivity::class.java)
            }
            if (state is UiState.Error && (state.failure.status == 401 || !vm.hasSession())) {
                app.auth.logout()
                login(state.failure.message)
            }
        }
        findViewById<Button>(R.id.retry).setOnClickListener { vm.load() }
        findViewById<Button>(R.id.logout).setOnClickListener { vm.logout(); login() }
        if (vm.state.value is UiState.Idle) vm.load()
    }
}
