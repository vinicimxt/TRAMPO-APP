package com.example.trampo.ui.agendamentos
import android.os.Bundle
import android.content.Intent
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
import java.math.BigDecimal

class AgendamentosActivity : BaseActivity() {
    private lateinit var vm: AgendamentosViewModel
    private val professional get() = intent.getBooleanExtra("professional", false)
    private val visao get() = if (professional) "recebidos" else "meus"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layout(R.layout.activity_agendamentos)
        vm = ViewModelProvider(this, SimpleFactory { AgendamentosViewModel(app.agendamentos) })[AgendamentosViewModel::class.java]
        if (professional) findViewById<TextView>(R.id.page_title).setText(R.string.received_bookings)
        val adapter = AgendamentoAdapter(professional) {
            startActivity(Intent(this, DetalheAgendamentoActivity::class.java)
                .putExtra("id", it).putExtra("professional", professional))
        }
        findViewById<RecyclerView>(R.id.list).apply { layoutManager = LinearLayoutManager(this@AgendamentosActivity); this.adapter = adapter }
        vm.state.observe(this) { state ->
            render(state, R.id.retry)?.let {
                adapter.submitList(it)
                if (it.isEmpty()) findViewById<TextView>(R.id.message).text = if (professional)
                    getString(R.string.empty_received_bookings) else
                    getString(R.string.two_lines, getString(R.string.empty_bookings), getString(R.string.empty_bookings_hint))
                if (intent.getBooleanExtra("created", false)) {
                    findViewById<TextView>(R.id.message).setText(R.string.booking_created)
                    intent.removeExtra("created")
                }
            }
        }
        findViewById<Button>(R.id.retry).setOnClickListener { vm.load(visao) }
    }
    override fun onResume() {
        super.onResume()
        if (!app.session.token().isNullOrBlank()) vm.load(visao)
    }
}
class AgendamentoAdapter(private val professional: Boolean, private val click: (Int) -> Unit) :
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
            findViewById<TextView>(R.id.name).text = item.servicoNome?.takeIf(String::isNotBlank)
                ?: context.getString(R.string.service_unavailable)
            val modality = when (item.atendimento) {
                "Local" -> context.getString(R.string.local)
                "Domicilio" -> context.getString(R.string.domicilio)
                "Online" -> context.getString(R.string.online)
                else -> item.atendimento.orEmpty()
            }
            findViewById<TextView>(R.id.body).text = listOf(item.presentation(context), modality)
                .filter { it.isNotBlank() }.joinToString(" · ")
            findViewById<TextView>(R.id.status).bindStatus(item.status)
            findViewById<TextView>(R.id.description).text = if (professional) item.clienteNome.orEmpty() else item.profissionalNome.orEmpty()
            findViewById<Button>(R.id.details).setOnClickListener { click(item.id) }
        }
    }
}
class DetalheAgendamentoActivity : BaseActivity() {
    private lateinit var vm: DetalheAgendamentoViewModel
    private val bookingId get() = intent.getIntExtra("id", 0)
    private val professional get() = intent.getBooleanExtra("professional", false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layout(R.layout.activity_detalhe_agendamento)
        vm = ViewModelProvider(this, SimpleFactory { DetalheAgendamentoViewModel(app.agendamentos) })[DetalheAgendamentoViewModel::class.java]
        val cancel = findViewById<Button>(R.id.cancel)
        val complete = findViewById<Button>(R.id.complete_booking)
        val review = findViewById<Button>(R.id.review_booking)
        complete.setOnClickListener {
            AlertDialog.Builder(this).setTitle(R.string.complete_booking).setMessage(R.string.complete_question)
                .setNegativeButton(R.string.back,null).setPositiveButton(R.string.complete_booking) { _,_ -> vm.complete(bookingId) }.show()
        }
        review.setOnClickListener { open(com.example.trampo.ui.conta.AvaliacaoActivity::class.java, bookingId) }
        findViewById<Button>(R.id.confirm_booking_professional).setOnClickListener {
            AlertDialog.Builder(this).setTitle(R.string.confirm_booking).setMessage(R.string.confirm_professional_question)
                .setNegativeButton(R.string.back, null).setPositiveButton(R.string.confirm_booking) { _, _ -> vm.confirm(bookingId) }.show()
        }
        findViewById<Button>(R.id.refuse_booking).setOnClickListener {
            AlertDialog.Builder(this).setTitle(R.string.refuse_booking).setMessage(R.string.refuse_question)
                .setNegativeButton(R.string.back, null).setPositiveButton(R.string.refuse_booking) { _, _ -> vm.refuse(bookingId) }.show()
        }
        findViewById<Button>(R.id.finalize_booking).setOnClickListener { promptFinalization() }
        cancel.visibility = View.GONE
        vm.state.observe(this) { state ->
            render(state, R.id.retry)?.let { booking ->
                findViewById<TextView>(R.id.name).text = booking.servicoNome?.takeIf { it.isNotBlank() }
                    ?: getString(R.string.service_unavailable)
                findViewById<TextView>(R.id.status).bindStatus(booking.status)
                findViewById<TextView>(R.id.person_label).setText(if (professional) R.string.client else R.string.professional)
                field(R.id.person_section, R.id.person_value,
                    if (professional) booking.clienteNome else booking.profissionalNome)
                field(R.id.date_section, R.id.date_value, booking.presentation(this))
                val modality = when (booking.atendimento) {
                    "Local" -> getString(R.string.local)
                    "Domicilio" -> getString(R.string.domicilio)
                    "Online" -> getString(R.string.online)
                    else -> booking.atendimento
                }
                field(R.id.modality_section, R.id.modality_value, modality)
                field(R.id.location_section, R.id.location_value,
                    booking.enderecoAtendimento?.enderecoFormatado?.takeIf { it.isNotBlank() }
                        ?: booking.enderecoCliente?.takeIf { it.isNotBlank() }
                        ?: booking.local)
                field(R.id.notes_section, R.id.notes_value, booking.descricao)
                val currency = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))
                field(R.id.value_section, R.id.value_value, booking.valorFinal?.let { currency.format(it) }
                    ?: if (booking.tipoPreco == "Fixo") booking.precoBase?.let { currency.format(it) }
                    else getString(R.string.price_agree))
                findViewById<TextView>(R.id.value_label).setText(
                    if (booking.valorFinal != null) R.string.final_value_label else R.string.current_price_label)
                complete.visibility = if (!professional && booking.status == "AguardandoCliente") View.VISIBLE else View.GONE
                review.visibility = View.GONE
                if (!professional && booking.status == "Finalizado") vm.loadEvaluation(bookingId)
                cancel.visibility = if (booking.status in setOf("Pendente", "Confirmado")) View.VISIBLE else View.GONE
                findViewById<Button>(R.id.confirm_booking_professional).visibility =
                    if (professional && booking.status == "Pendente") View.VISIBLE else View.GONE
                findViewById<Button>(R.id.refuse_booking).visibility =
                    if (professional && booking.status == "Pendente") View.VISIBLE else View.GONE
                findViewById<Button>(R.id.finalize_booking).visibility =
                    if (professional && booking.status == "Confirmado") View.VISIBLE else View.GONE
            }
        }
        vm.evaluation.observe(this) { state ->
            review.visibility = if (!professional && (state as? UiState.Success)?.value?.podeAvaliar == true)
                View.VISIBLE else View.GONE
        }
        vm.cancellation.observe(this) { state ->
            if (state !is UiState.Idle) render(state, R.id.cancel, R.id.retry, R.id.complete_booking,
                R.id.review_booking, R.id.confirm_booking_professional, R.id.refuse_booking, R.id.finalize_booking)
            if (state is UiState.Success) findViewById<TextView>(R.id.message).setText(R.string.booking_updated)
            if (state is UiState.Error) {
                listOf(R.id.cancel, R.id.complete_booking, R.id.confirm_booking_professional,
                    R.id.refuse_booking, R.id.finalize_booking).forEach { findViewById<Button>(it).isEnabled = false }
            }
        }
        cancel.setOnClickListener {
            AlertDialog.Builder(this).setTitle(R.string.cancel_booking).setMessage(R.string.cancel_question)
                .setNegativeButton(R.string.keep_booking, null)
                .setPositiveButton(R.string.cancel_booking) { _, _ -> vm.cancel(bookingId) }.show()
        }
        findViewById<Button>(R.id.retry).setOnClickListener {
            vm.cancellation.value = UiState.Idle
            cancel.isEnabled = true; complete.isEnabled = true
            listOf(R.id.confirm_booking_professional, R.id.refuse_booking, R.id.finalize_booking)
                .forEach { findViewById<Button>(it).isEnabled = true }
            vm.load(bookingId)
        }
        if (vm.state.value is UiState.Idle) vm.load(bookingId)
    }

    override fun onResume() {
        super.onResume()
        if (::vm.isInitialized && !app.session.token().isNullOrBlank()) vm.load(bookingId)
    }

    private fun field(section: Int, value: Int, text: String?) {
        findViewById<View>(section).visibility = if (text.isNullOrBlank()) View.GONE else View.VISIBLE
        findViewById<TextView>(value).text = text.orEmpty()
    }

    private fun promptFinalization() {
        val booking = (vm.state.value as? UiState.Success<AgendamentoResponse>)?.value ?: return
        if (booking.tipoPreco == "Fixo") {
            AlertDialog.Builder(this).setTitle(R.string.finalize_booking).setMessage(R.string.finalize_question)
                .setNegativeButton(R.string.back, null)
                .setPositiveButton(R.string.finalize_booking) { _, _ -> vm.finalize(bookingId, BigDecimal.ZERO) }.show()
            return
        }
        val input = EditText(this).apply { inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = getString(R.string.final_value_hint) }
        AlertDialog.Builder(this).setTitle(R.string.finalize_booking).setView(input)
            .setNegativeButton(R.string.back, null).setPositiveButton(R.string.finalize_booking) { _, _ ->
                val value = input.text.toString().replace(',', '.').toBigDecimalOrNull()
                if (value == null || value <= BigDecimal.ZERO) {
                    findViewById<TextView>(R.id.message).setText(R.string.error_validation)
                } else vm.finalize(bookingId, value)
            }.show()
    }
}
