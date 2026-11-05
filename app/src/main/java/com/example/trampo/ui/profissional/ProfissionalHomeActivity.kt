package com.example.trampo.ui.profissional

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.example.trampo.R
import com.example.trampo.ui.*
import com.example.trampo.ui.agendamentos.AgendamentosActivity
import com.example.trampo.ui.home.HomeViewModel

class ProfissionalHomeActivity : BaseActivity() {
    private lateinit var vm: HomeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layout(R.layout.activity_profissional_home)
        vm = ViewModelProvider(this, SimpleFactory { HomeViewModel(app.auth) })[HomeViewModel::class.java]
        vm.state.observe(this) { state ->
            render(state, R.id.retry)?.let { user ->
                if (user.tipo != "profissional") {
                    route(com.example.trampo.ui.home.HomeActivity::class.java)
                    return@let
                }
                findViewById<TextView>(R.id.greeting).text = getString(R.string.greeting, user.nome)
            }
            val ready = state is UiState.Success && state.value.tipo == "profissional"
            listOf(R.id.bookings, R.id.services, R.id.locations, R.id.availability, R.id.my_profile,
                R.id.my_reviews, R.id.notifications, R.id.support).forEach { findViewById<Button>(it).isEnabled = ready }
        }
        findViewById<Button>(R.id.bookings).setOnClickListener {
            startActivity(Intent(this, AgendamentosActivity::class.java).putExtra("professional", true))
        }
        findViewById<Button>(R.id.services).setOnClickListener { open(ProfissionalServicosActivity::class.java) }
        findViewById<Button>(R.id.locations).setOnClickListener { open(ProfissionalLocaisActivity::class.java) }
        findViewById<Button>(R.id.availability).setOnClickListener {
            startActivity(Intent(this, ProfissionalServicosActivity::class.java).putExtra("availability", true))
        }
        findViewById<Button>(R.id.my_profile).setOnClickListener { open(com.example.trampo.ui.conta.PerfilActivity::class.java) }
        findViewById<Button>(R.id.my_reviews).setOnClickListener { open(ProfissionalAvaliacoesActivity::class.java) }
        findViewById<Button>(R.id.notifications).setOnClickListener { open(com.example.trampo.ui.conta.NotificacoesActivity::class.java) }
        findViewById<Button>(R.id.support).setOnClickListener { open(com.example.trampo.ui.conta.SuporteActivity::class.java) }
        findViewById<Button>(R.id.retry).setOnClickListener { vm.load() }
        findViewById<Button>(R.id.logout).setOnClickListener { vm.logout(); login() }
        if (vm.state.value is UiState.Idle) vm.load()
    }

    override fun onResume() {
        super.onResume()
        if (::vm.isInitialized && !app.session.token().isNullOrBlank()) vm.load()
    }
}
