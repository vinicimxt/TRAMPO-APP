package com.example.trampo.ui.agendamentos
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.*
import com.example.trampo.R
import com.example.trampo.data.model.AgendamentoResponse
import com.example.trampo.ui.*
import java.text.NumberFormat
import java.util.Locale

class AgendamentosActivity : BaseActivity() {
    private lateinit var vm: AgendamentosViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layout(R.layout.activity_agendamentos)
        vm = ViewModelProvider(this, SimpleFactory { AgendamentosViewModel(app.agendamentos) })[AgendamentosViewModel::class.java]
        val adapter = AgendamentoAdapter { open(DetalheAgendamentoActivity::class.java, it) }
        findViewById<RecyclerView>(R.id.list).apply { layoutManager = LinearLayoutManager(this@AgendamentosActivity); this.adapter = adapter }
        vm.state.observe(this) { state ->
            render(state, R.id.retry)?.let {
                adapter.submitList(it)
                if (it.isEmpty()) findViewById<TextView>(R.id.message).text = getString(R.string.two_lines, getString(R.string.empty_bookings), getString(R.string.empty_bookings_hint))
                if (intent.getBooleanExtra("created", false)) {
                    findViewById<TextView>(R.id.message).setText(R.string.booking_created)
                    intent.removeExtra("created")
                }
            }
        }
        findViewById<Button>(R.id.retry).setOnClickListener { vm.load() }
    }
    override fun onResume() {
        super.onResume()
        if (!app.session.token().isNullOrBlank()) vm.load()
    }
}
class AgendamentoAdapter(private val click: (Int) -> Unit) :
    androidx.recyclerview.widget.ListAdapter<AgendamentoResponse, AgendamentoAdapter.Holder>(object : DiffUtil.ItemCallback<AgendamentoResponse>() {
        override fun areItemsTheSame(a: AgendamentoResponse, b: AgendamentoResponse) = a.id == b.id
        override fun areContentsTheSame(a: AgendamentoResponse, b: AgendamentoResponse) = a == b
    }) {
    class Holder(view: View) : RecyclerView.ViewHolder(view)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_agendamento, parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position)
        holder.itemView.apply {
            findViewById<TextView>(R.id.name).text = context.getString(R.string.service_reference, item.servicoId)
            findViewById<TextView>(R.id.body).text = item.presentation(context)
            findViewById<TextView>(R.id.status).bindStatus(item.status)
            findViewById<TextView>(R.id.description).text = context.getString(R.string.booking_reference, item.id)
            findViewById<Button>(R.id.details).setOnClickListener { click(item.id) }
        }
    }
}
class DetalheAgendamentoActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layout(R.layout.activity_detalhe_agendamento)
        val id = intent.getIntExtra("id", 0)
        val vm = ViewModelProvider(this, SimpleFactory { DetalheAgendamentoViewModel(app.agendamentos) })[DetalheAgendamentoViewModel::class.java]
        val cancel = findViewById<Button>(R.id.cancel)
        val complete = findViewById<Button>(R.id.complete_booking)
        val review = findViewById<Button>(R.id.review_booking)
        complete.setOnClickListener {
            AlertDialog.Builder(this).setTitle(R.string.complete_booking).setMessage(R.string.complete_question)
                .setNegativeButton(R.string.back,null).setPositiveButton(R.string.complete_booking) { _,_ -> vm.complete(id) }.show()
        }
        review.setOnClickListener { open(com.example.trampo.ui.conta.AvaliacaoActivity::class.java,id) }
        cancel.visibility = View.GONE
        vm.state.observe(this) { state ->
            render(state, R.id.retry)?.let {
                findViewById<TextView>(R.id.name).text = getString(R.string.service_reference, it.servicoId)
                findViewById<TextView>(R.id.body).text = it.presentation(this)
                findViewById<TextView>(R.id.status).bindStatus(it.status)
                val price = it.valorFinal?.let { value ->
                    getString(R.string.final_value, NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(value))
                } ?: getString(R.string.value_pending)
                findViewById<TextView>(R.id.description).text = listOfNotNull(
                    it.descricao, it.enderecoAtendimento?.enderecoFormatado ?: it.enderecoCliente, price
                ).filter { text -> text.isNotBlank() }.joinToString("\n\n")
                complete.visibility = if (it.status == "AguardandoCliente") View.VISIBLE else View.GONE
                review.visibility = if (it.status == "Finalizado") View.VISIBLE else View.GONE
                cancel.visibility = if (it.status in setOf("Pendente", "Confirmado")) View.VISIBLE else View.GONE
            }
        }
        vm.cancellation.observe(this) { state ->
            if (state !is UiState.Idle) render(state, R.id.cancel, R.id.retry, R.id.complete_booking, R.id.review_booking)

            if (state is UiState.Success) {
                val status = (vm.state.value as? UiState.Success<AgendamentoResponse>)?.value?.status
                findViewById<TextView>(R.id.message).setText(
                    if (status == "Finalizado") R.string.conclusion_confirmed else R.string.canceled)
            }
            if (state is UiState.Error) { cancel.isEnabled = false; complete.isEnabled = false }
        }
        cancel.setOnClickListener {
            AlertDialog.Builder(this).setTitle(R.string.cancel_booking).setMessage(R.string.cancel_question)
                .setNegativeButton(R.string.keep_booking, null)
                .setPositiveButton(R.string.cancel_booking) { _, _ -> vm.cancel(id) }.show()
        }
        findViewById<Button>(R.id.retry).setOnClickListener {
            vm.cancellation.value = UiState.Idle
            cancel.isEnabled = true; complete.isEnabled = true
            vm.load(id)
        }
        if (vm.state.value is UiState.Idle) vm.load(id)
    }
}