package com.example.trampo.ui

import android.content.Context
import com.example.trampo.R
import com.example.trampo.data.model.*
import java.text.NumberFormat
import java.util.Locale

fun ServicoResponse.modality(context: Context): String = when (atendimento) {
    "Domicilio" -> context.getString(R.string.domicilio)
    "Online" -> context.getString(R.string.online)
    "Local" -> context.getString(R.string.local)
    else -> atendimento
}
fun ServicoResponse.price(context: Context): String =
    if (tipoPreco == "Fixo" && precoBase != null)
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(precoBase)
    else context.getString(R.string.price_agree)
fun ServicoResponse.presentation(context: Context): String =
    context.getString(R.string.two_lines, modality(context), price(context))

fun android.view.View.bindService(service: ServicoResponse) {
    findViewById<android.widget.TextView>(R.id.modality).apply {
        text = service.modality(context)
        val icon = when (service.atendimento) {
            "Domicilio" -> R.drawable.ic_home
            "Online" -> R.drawable.ic_online
            "Local" -> R.drawable.ic_location
            else -> 0
        }
        setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0)
        compoundDrawablePadding = (8 * resources.displayMetrics.density).toInt()
    }
    findViewById<android.widget.TextView>(R.id.price).text = service.price(context)
    findViewById<android.widget.TextView>(R.id.body).apply {
        text = if (service.ativo) "" else context.getString(R.string.service_unavailable)
        visibility = if (text.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
    }
    findViewById<android.widget.TextView>(R.id.description).apply {
        text = service.descricao
        visibility = if (text.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE
    }
}
fun android.widget.TextView.bindStatus(status: String) {
    val label = when (status) {
        "Pendente" -> R.string.status_pending
        "Confirmado" -> R.string.status_confirmed
        "Finalizado" -> R.string.status_done
        "Cancelado" -> R.string.status_canceled
        "AguardandoCliente" -> R.string.status_waiting_client
        "CanceladoCliente" -> R.string.status_canceled_client
        "CanceladoProfissional" -> R.string.status_canceled_professional
        else -> null
    }
    text = label?.let(context::getString) ?: status
    val (foreground, surface) = when (status) {
        "Pendente", "AguardandoCliente" -> R.color.warning to R.color.warning_soft
        "Confirmado", "Finalizado" -> R.color.success to R.color.success_soft
        "Cancelado", "CanceladoCliente", "CanceladoProfissional" -> R.color.error to R.color.error_soft
        else -> R.color.brand to R.color.brand_soft
    }
    setTextColor(androidx.core.content.ContextCompat.getColor(context, foreground))
    backgroundTintList = android.content.res.ColorStateList.valueOf(
        androidx.core.content.ContextCompat.getColor(context, surface))
}
fun displayDate(value: String): String {
    val parts = value.split("-")
    return if (parts.size == 3) parts[2] + "/" + parts[1] + "/" + parts[0] else value
}
fun AgendamentoResponse.presentation(context: Context): String =
    context.getString(R.string.booking_when, displayDate(data), hora.take(5))