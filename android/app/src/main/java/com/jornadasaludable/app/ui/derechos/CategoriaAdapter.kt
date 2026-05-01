package com.jornadasaludable.app.ui.derechos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jornadasaludable.app.R
import com.jornadasaludable.app.data.api.dto.CategoriaDto

class CategoriaAdapter(
    private val onClick: (CategoriaDto) -> Unit,
) : RecyclerView.Adapter<CategoriaAdapter.VH>() {

    private var items: List<CategoriaDto> = emptyList()
    private var counts: Map<String, Int> = emptyMap()

    fun submit(items: List<CategoriaDto>, counts: Map<String, Int>) {
        this.items = items
        this.counts = counts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_categoria, parent, false)
        return VH(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val cat = items[position]
        holder.bind(cat, counts[cat.codigo] ?: 0)
        holder.itemView.setOnClickListener { onClick(cat) }
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val icon:    ImageView = view.findViewById(R.id.ivIcono)
        private val tvNombre: TextView  = view.findViewById(R.id.tvNombre)
        private val tvCount:  TextView  = view.findViewById(R.id.tvCount)

        fun bind(c: CategoriaDto, count: Int) {
            tvNombre.text = c.nombre
            tvCount.text = if (count == 1) "1 artículo" else "$count artículos"
            // Mapeo codigo → drawable; fallback a un icono genérico.
            val iconRes = when (c.codigo) {
                "JORNADA"     -> R.drawable.ic_cat_jornada
                "DESCANSOS"   -> R.drawable.ic_cat_descansos
                "HORAS_EXTRA" -> R.drawable.ic_cat_horas_extra
                "REGISTRO"    -> R.drawable.ic_cat_registro
                else          -> R.drawable.ic_cat_jornada
            }
            icon.setImageResource(iconRes)
        }
    }
}
