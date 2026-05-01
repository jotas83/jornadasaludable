package com.jornadasaludable.app.ui.alertas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.jornadasaludable.app.R
import com.jornadasaludable.app.data.api.dto.AlertaDto

class AlertaListAdapter(
    private val onMarcarLeida: (uuid: String) -> Unit,
) : RecyclerView.Adapter<AlertaListAdapter.VH>() {

    private var items: List<AlertaDto> = emptyList()
    private var markingUuid: String? = null

    fun submit(newItems: List<AlertaDto>, markingUuid: String?) {
        this.items = newItems
        this.markingUuid = markingUuid
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_alerta, parent, false)
        return VH(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], markingUuid)
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val card:        MaterialCardView = view.findViewById(R.id.cardAlerta)
        private val tvNivel:     TextView         = view.findViewById(R.id.tvNivel)
        private val tvTipo:      TextView         = view.findViewById(R.id.tvTipo)
        private val tvMensaje:   TextView         = view.findViewById(R.id.tvMensaje)
        private val tvBaseLegal: TextView         = view.findViewById(R.id.tvBaseLegal)
        private val tvFecha:     TextView         = view.findViewById(R.id.tvFecha)
        private val btnMarcar:   MaterialButton   = view.findViewById(R.id.btnMarcarLeida)
        private val progress:    ProgressBar      = view.findViewById(R.id.progressMarcar)
        private val tvLeida:     TextView         = view.findViewById(R.id.tvLeida)

        fun bind(a: AlertaDto, markingUuid: String?) {
            val ctx = itemView.context
            tvNivel.text     = a.nivel
            tvTipo.text      = a.tipoNombre
            tvMensaje.text   = a.mensaje
            tvBaseLegal.text = a.baseLegal ?: ""
            tvBaseLegal.isVisible = !a.baseLegal.isNullOrBlank()
            tvFecha.text     = a.fechaGeneracion

            // Color del badge según nivel; FICHAJE_INCOMPLETO se sobreescribe en morado.
            val nivelColorRes = when (a.tipo) {
                "FICHAJE_INCOMPLETO" -> R.color.alerta_fichaje_incompleto
                else -> when (a.nivel) {
                    "CRITICA"     -> R.color.alerta_critica
                    "GRAVE"       -> R.color.alerta_grave
                    "AVISO"       -> R.color.alerta_aviso
                    "INFORMATIVA" -> R.color.alerta_informativa
                    else          -> R.color.alerta_aviso
                }
            }
            val color = ContextCompat.getColor(ctx, nivelColorRes)
            tvNivel.setBackgroundColor(color)
            card.setStrokeColor(color)

            // Botón marcar leída / estado leída / spinner si en curso
            val isMarking = markingUuid == a.uuid
            progress.isVisible = isMarking
            when {
                a.leida -> {
                    btnMarcar.isVisible = false
                    tvLeida.isVisible = true
                }
                isMarking -> {
                    btnMarcar.isVisible = false
                    tvLeida.isVisible = false
                }
                else -> {
                    btnMarcar.isVisible = true
                    tvLeida.isVisible = false
                    btnMarcar.setOnClickListener { onMarcarLeida(a.uuid) }
                }
            }
        }
    }
}
