package com.example.trampo.ui.profissional

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.example.trampo.R
import com.example.trampo.ui.*

class ProfissionalLocaisActivity : BaseActivity() {
    private lateinit var vm: ProfissionalLocaisViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layout(R.layout.activity_profissional_locais)
        vm = ViewModelProvider(this, SimpleFactory { ProfissionalLocaisViewModel(app.profissional) })[ProfissionalLocaisViewModel::class.java]
        vm.state.observe(this) { state ->
            render(state, R.id.retry)?.let { locais ->
                val list = findViewById<LinearLayout>(R.id.locations_list)
                list.removeAllViews()
                if (locais.isEmpty()) findViewById<TextView>(R.id.message).setText(R.string.empty_locations)
                locais.forEach { local ->
                    val card = layoutInflater.inflate(R.layout.item_local_proprio, list, false)
                    card.findViewById<TextView>(R.id.name).text = local.nome.ifBlank { getString(R.string.location_label) }
                    card.findViewById<TextView>(R.id.address).text = local.endereco
                    card.findViewById<Button>(R.id.edit).setOnClickListener { open(LocalEditorActivity::class.java, local.id) }
                    card.findViewById<Button>(R.id.remove).setOnClickListener {
                        AlertDialog.Builder(this).setTitle(R.string.remove_location)
                            .setMessage(R.string.remove_location_question).setNegativeButton(R.string.back, null)
                            .setPositiveButton(R.string.remove_location) { _, _ -> vm.remove(local.id) }.show()
                    }
                    list.addView(card)
                }
            }
        }
        vm.removal.observe(this) { state ->
            if (state !is UiState.Idle) render(state, R.id.new_location)
            if (state is UiState.Error && state.failure.status == 409) findViewById<TextView>(R.id.message).setText(R.string.location_in_use)
            if (state is UiState.Success) findViewById<TextView>(R.id.message).setText(R.string.location_removed)
            if (state is UiState.Error) findViewById<Button>(R.id.new_location).isEnabled = false
        }
        findViewById<Button>(R.id.new_location).setOnClickListener { open(LocalEditorActivity::class.java) }
        findViewById<Button>(R.id.retry).setOnClickListener {
            vm.removal.value = UiState.Idle
            findViewById<Button>(R.id.new_location).isEnabled = true
            vm.load()
        }
    }
    override fun onResume() {
        super.onResume()
        if (::vm.isInitialized && !app.session.token().isNullOrBlank()) vm.load()
    }
}
