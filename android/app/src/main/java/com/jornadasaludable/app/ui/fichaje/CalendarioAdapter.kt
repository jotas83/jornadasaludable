package com.jornadasaludable.app.ui.fichaje

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.jornadasaludable.app.R
import com.jornadasaludable.app.data.api.dto.JornadaListItemDto
import java.time.LocalDate
import java.time.YearMonth

// Rejilla 7×N con los días del mes. El color de la celda viene del estado de la jornada.
class CalendarioAdapter(
    private val onDiaClick: (LocalDate, JornadaListItemDto?) -> Unit,
) : RecyclerView.Adapter<CalendarioAdapter.DiaViewHolder>() {

    private var mes: YearMonth = YearMonth.now()
    private var jornadasPorFecha: Map<String, JornadaListItemDto> = emptyMap()
    private var primeraCeldaOffset: Int = 0
    private var diasMes: Int = 0
    private var totalCeldas: Int = 0

    fun submit(mes: YearMonth, jornadasPorFecha: Map<String, JornadaListItemDto>) {
        this.mes = mes
        this.jornadasPorFecha = jornadasPorFecha
        this.diasMes = mes.lengthOfMonth()
        // Lunes=0 ... domingo=6 para alinear la primera fila.
        this.primeraCeldaOffset = mes.atDay(1).dayOfWeek.value - 1
        this.totalCeldas = (((primeraCeldaOffset + diasMes) + 6) / 7) * 7
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendario_dia, parent, false)
        return DiaViewHolder(view)
    }

    override fun getItemCount(): Int = totalCeldas

    override fun onBindViewHolder(holder: DiaViewHolder, position: Int) {
        val ctx = holder.itemView.context
        val dia = position - primeraCeldaOffset + 1

        if (dia < 1 || dia > diasMes) {
            holder.tvDia.text = ""
            holder.tvDia.setBackgroundColor(Color.TRANSPARENT)
            holder.itemView.isClickable = false
            holder.itemView.setOnClickListener(null)
            return
        }

        val fecha = mes.atDay(dia)
        val fechaStr = fecha.toString()
        val jornada = jornadasPorFecha[fechaStr]

        holder.tvDia.text = dia.toString()
        val colorRes = when (jornada?.estado) {
            "VALIDADA"   -> R.color.cal_validada
            "CERRADA"    -> R.color.cal_cerrada
            "INCOMPLETA" -> R.color.cal_incompleta
            "CORREGIDA"  -> R.color.cal_incompleta
            "ABIERTA"    -> R.color.cal_abierta
            else         -> R.color.cal_sin_jornada
        }
        holder.tvDia.setBackgroundColor(ContextCompat.getColor(ctx, colorRes))
        holder.tvDia.setTypeface(null, if (fecha == LocalDate.now()) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)

        holder.itemView.isClickable = true
        holder.itemView.setOnClickListener { onDiaClick(fecha, jornada) }
    }

    class DiaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDia: TextView = view.findViewById(R.id.tvDia)
    }
}
