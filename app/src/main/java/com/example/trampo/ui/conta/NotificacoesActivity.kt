package com.example.trampo.ui.conta

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.*
import com.example.trampo.R
import com.example.trampo.data.model.NotificacaoResponse
import com.example.trampo.ui.*

class NotificacoesActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); layout(R.layout.activity_notificacoes)
        val vm = ViewModelProvider(this, SimpleFactory { NotificacoesViewModel(app.notificacoes) })[NotificacoesViewModel::class.java]
        val adapter = NotificacaoAdapter { vm.ler(it) }
        findViewById<RecyclerView>(R.id.list).apply { layoutManager = LinearLayoutManager(this@NotificacoesActivity); this.adapter = adapter }
        vm.state.observe(this) { state ->
            render(state, R.id.more, R.id.retry)?.let {
                adapter.submitList(it)
                if (it.isEmpty()) findViewById<TextView>(R.id.message).setText(R.string.empty_notifications)
            }
            adapter.busy = state is UiState.Loading
            if (adapter.itemCount > 0) adapter.notifyItemRangeChanged(0, adapter.itemCount)
            findViewById<Button>(R.id.more).visibility = if (vm.hasMore) View.VISIBLE else View.GONE
        }
        findViewById<Button>(R.id.more).setOnClickListener { vm.load() }
        findViewById<Button>(R.id.retry).setOnClickListener { vm.load(true) }
        if (vm.state.value is UiState.Idle) vm.load()
    }
}
class NotificacaoAdapter(private val click: (Int) -> Unit) :
    androidx.recyclerview.widget.ListAdapter<NotificacaoResponse, NotificacaoAdapter.Holder>(object : DiffUtil.ItemCallback<NotificacaoResponse>() {
        override fun areItemsTheSame(a: NotificacaoResponse,b: NotificacaoResponse) = a.id == b.id
        override fun areContentsTheSame(a: NotificacaoResponse,b: NotificacaoResponse) = a == b
    }) {
    var busy = false
    class Holder(view: View) : RecyclerView.ViewHolder(view)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_notificacao,parent,false))
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position)
        holder.itemView.apply {
            findViewById<TextView>(R.id.name).text = item.titulo
            findViewById<TextView>(R.id.body).text = item.mensagem
            findViewById<TextView>(R.id.date_label).text = if (item.dataCriacao.length >= 16 && item.dataCriacao[4] == '-')
                "${item.dataCriacao.substring(8,10)}/${item.dataCriacao.substring(5,7)}/${item.dataCriacao.substring(0,4)} ${item.dataCriacao.substring(11,16)}"
            else item.dataCriacao
            findViewById<TextView>(R.id.status).setText(if(item.lida) R.string.notification_read else R.string.notification_unread)
            findViewById<Button>(R.id.read_notification).apply {
                visibility = if(item.lida) View.GONE else View.VISIBLE
                isEnabled = !busy
                setOnClickListener { click(item.id) }
            }
        }
    }
}
