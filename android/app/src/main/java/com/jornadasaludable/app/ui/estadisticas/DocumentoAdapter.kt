package com.jornadasaludable.app.ui.estadisticas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.jornadasaludable.app.R
import com.jornadasaludable.app.data.api.dto.DocumentoDto

class DocumentoAdapter(
    private val onClick: (DocumentoDto) -> Unit,
) : RecyclerView.Adapter<DocumentoAdapter.VH>() {

    private var items: List<DocumentoDto> = emptyList()
    private var downloadingUuid: String? = null

    fun submit(newItems: List<DocumentoDto>, downloadingUuid: String? = null) {
        this.items = newItems
        this.downloadingUuid = downloadingUuid
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_documento, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val doc = items[position]
        holder.bind(doc, isDownloading = doc.uuid == downloadingUuid)
        holder.itemView.setOnClickListener { onClick(doc) }
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvNombre:  TextView    = view.findViewById(R.id.tvNombre)
        private val tvPeriodo: TextView    = view.findViewById(R.id.tvPeriodo)
        private val tvCreated: TextView    = view.findViewById(R.id.tvCreated)
        private val progress:  ProgressBar = view.findViewById(R.id.progressDownload)

        fun bind(d: DocumentoDto, isDownloading: Boolean) {
            tvNombre.text  = d.nombreFichero
            tvPeriodo.text = listOfNotNull(d.periodoDesde, d.periodoHasta).joinToString(" — ")
            tvCreated.text = d.createdAt
            progress.isVisible = isDownloading
        }
    }
}
