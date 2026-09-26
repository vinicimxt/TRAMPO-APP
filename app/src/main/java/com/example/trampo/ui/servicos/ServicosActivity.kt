package com.example.trampo.ui.servicos
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.*
import com.example.trampo.R
import com.example.trampo.data.model.ServicoResponse
import com.example.trampo.ui.*
import com.example.trampo.ui.agendamentos.NovoAgendamentoActivity

class ServicosActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layout(R.layout.activity_servicos)
        val vm = ViewModelProvider(this, SimpleFactory { ServicosViewModel(app.servicos) })[ServicosViewModel::class.java]
        val search = findViewById<EditText>(R.id.search_text)
        val modality = findViewById<Spinner>(R.id.filter_modality)
        val modalities = listOf(null,"Online","Local","Domicilio")
        modality.adapter = ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,
            listOf(getString(R.string.all_modalities),getString(R.string.online),getString(R.string.local),getString(R.string.domicilio)))
        search.setText(vm.busca)
        modality.setSelection(modalities.indexOf(vm.atendimento).coerceAtLeast(0))
        findViewById<Button>(R.id.search).setOnClickListener {
            vm.search(search.text.toString(),modalities[modality.selectedItemPosition])
            (getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager).hideSoftInputFromWindow(search.windowToken,0)
            search.clearFocus()
        }
        search.setOnEditorActionListener { _,action,_ ->
            if(action == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) { findViewById<Button>(R.id.search).performClick(); true } else false
        }
        val adapter = ServicoAdapter { open(DetalheServicoActivity::class.java, it) }
        findViewById<RecyclerView>(R.id.list).apply { layoutManager = LinearLayoutManager(this@ServicosActivity); this.adapter = adapter }
        vm.state.observe(this) { state ->
            render(state, R.id.more, R.id.retry, R.id.search)?.let {
                adapter.submitList(it)
                if (it.isEmpty()) findViewById<TextView>(R.id.message).text = getString(R.string.two_lines, getString(R.string.empty_services), getString(R.string.empty_services_hint))
            }
            findViewById<Button>(R.id.more).visibility = if (vm.hasMore) View.VISIBLE else View.GONE
        }
        findViewById<Button>(R.id.more).setOnClickListener { vm.load() }
        findViewById<Button>(R.id.retry).setOnClickListener { vm.load(reset = true) }
        if (vm.state.value is UiState.Idle) vm.load()
    }
}
class ServicoAdapter(private val click: (Int) -> Unit) :
    androidx.recyclerview.widget.ListAdapter<ServicoResponse, ServicoAdapter.Holder>(object : DiffUtil.ItemCallback<ServicoResponse>() {
        override fun areItemsTheSame(a: ServicoResponse, b: ServicoResponse) = a.id == b.id
        override fun areContentsTheSame(a: ServicoResponse, b: ServicoResponse) = a == b
    }) {
    class Holder(view: View) : RecyclerView.ViewHolder(view)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_servico, parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position)
        holder.itemView.apply {
            findViewById<TextView>(R.id.name).text = item.nome
            bindService(item)
            findViewById<Button>(R.id.details).setOnClickListener { click(item.id) }
        }
    }
}
class DetalheServicoActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layout(R.layout.activity_detalhe_servico)
        val id = intent.getIntExtra("id", 0)
        val vm = ViewModelProvider(this, SimpleFactory { DetalheServicoViewModel(app.servicos) })[DetalheServicoViewModel::class.java]
        vm.state.observe(this) { state ->
            findViewById<Button>(R.id.times).isEnabled = state is UiState.Success && state.value.ativo
            render(state, R.id.retry)?.let {
                findViewById<TextView>(R.id.name).text = it.nome
                findViewById<View>(R.id.root).bindService(it)
                findViewById<TextView>(R.id.professional_info).text = listOfNotNull(it.nomeProfissional,it.categoria,it.subcategoria).filter { value -> value.isNotBlank() }.joinToString("\n")
            }
        }
        findViewById<Button>(R.id.times).setOnClickListener { open(NovoAgendamentoActivity::class.java, id) }
        findViewById<Button>(R.id.retry).setOnClickListener { vm.load(id) }
        if (vm.state.value is UiState.Idle) vm.load(id)
    }
}