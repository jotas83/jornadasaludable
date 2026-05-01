package com.jornadasaludable.app.ui.estadisticas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jornadasaludable.app.R
import com.jornadasaludable.app.data.api.dto.DocumentoDto

class DocumentoAdapter : RecyclerView.Adapter<DocumentoAdapter.VH>() {
    private var items: List<DocumentoDto> = emptyList()

    fun submit(newItems: List<DocumentoDto>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_documento, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvNombre: TextView = view.findViewById(R.id.tvNombre)
        private val tvPeriodo: TextView = view.findViewById(R.id.tvPeriodo)
        private val tvCreated: TextView = view.findViewById(R.id.tvCreated)

        fun bind(d: DocumentoDto) {
            tvNombre.text = d.nombreFichero
            tvPeriodo.text = listOfNotNull(d.periodoDesde, d.periodoHasta).joinToString(" — ")
            tvCreated.text = d.createdAt
        }
    }
}
