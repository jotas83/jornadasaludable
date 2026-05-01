package com.jornadasaludable.app.ui.derechos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jornadasaludable.app.R
import com.jornadasaludable.app.data.api.dto.DerechoListItemDto

class DerechoListAdapter(
    private val onClick: (DerechoListItemDto) -> Unit,
) : RecyclerView.Adapter<DerechoListAdapter.VH>() {

    private var items: List<DerechoListItemDto> = emptyList()

    fun submit(items: List<DerechoListItemDto>) {
        this.items = items
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_derecho_list, parent, false)
        return VH(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
        holder.itemView.setOnClickListener { onClick(items[position]) }
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTitulo:   TextView = view.findViewById(R.id.tvTitulo)
        private val tvArticulo: TextView = view.findViewById(R.id.tvArticulo)
        private val tvResumen:  TextView = view.findViewById(R.id.tvResumen)

        fun bind(d: DerechoListItemDto) {
            tvTitulo.text   = d.titulo
            tvArticulo.text = d.articuloReferencia
            tvResumen.text  = d.resumen
        }
    }
}
