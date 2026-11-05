package com.example.trampo.ui.home

import android.os.Bundle
import android.widget.*
import androidx.lifecycle.ViewModelProvider
import com.example.trampo.R
import com.example.trampo.ui.*
import com.example.trampo.ui.servicos.ServicosActivity
import com.example.trampo.ui.agendamentos.AgendamentosActivity

class HomeActivity : BaseActivity() {
    private lateinit var vm: HomeViewModel
    override fun onResume() { super.onResume(); if (!app.session.token().isNullOrBlank()) vm.load() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layout(R.layout.activity_home)
        vm = ViewModelProvider(this, SimpleFactory { HomeViewModel(app.auth) })[HomeViewModel::class.java]
        vm.state.observe(this) { state ->
            render(state, R.id.retry)?.let {
                if (it.tipo == "profissional") { route(com.example.trampo.ui.profissional.ProfissionalHomeActivity::class.java); return@let }
                findViewById<TextView>(R.id.greeting).text = getString(R.string.greeting, it.nome)
            }
            if (state is UiState.Error && !vm.hasSession()) login(state.failure.message)
            val ready = state is UiState.Success
            findViewById<Button>(R.id.services).isEnabled = ready
            findViewById<Button>(R.id.bookings).isEnabled = ready
        }
        findViewById<Button>(R.id.my_profile).setOnClickListener { open(com.example.trampo.ui.conta.PerfilActivity::class.java) }
        findViewById<Button>(R.id.notifications).setOnClickListener { open(com.example.trampo.ui.conta.NotificacoesActivity::class.java) }
        findViewById<Button>(R.id.support).setOnClickListener { open(com.example.trampo.ui.conta.SuporteActivity::class.java) }
        findViewById<Button>(R.id.services).setOnClickListener { open(ServicosActivity::class.java) }
        findViewById<Button>(R.id.bookings).setOnClickListener { open(AgendamentosActivity::class.java) }
        findViewById<Button>(R.id.logout).setOnClickListener { vm.logout(); login() }
        findViewById<Button>(R.id.retry).setOnClickListener { vm.load() }

    }
}
