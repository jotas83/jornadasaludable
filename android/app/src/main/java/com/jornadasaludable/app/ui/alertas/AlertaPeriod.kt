package com.jornadasaludable.app.ui.alertas

import com.jornadasaludable.app.data.api.dto.AlertaDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class AlertaPeriod(val tabIndex: Int) {
    HOY(0), SEMANA(1), MES(2);

    companion object {
        fun fromTabIndex(i: Int): AlertaPeriod = entries.firstOrNull { it.tabIndex == i } ?: HOY
    }
}

private val backendDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

/**
 * Filtra alertas por período usando `fecha_generacion`. El formato del
 * backend es "YYYY-MM-DD HH:mm:ss" (DATETIME). Si el parseo falla (formato
 * inesperado), la alerta se incluye igualmente en el período más amplio (MES).
 */
fun List<AlertaDto>.filterByPeriod(period: AlertaPeriod): List<AlertaDto> {
    val today = LocalDate.now()
    val limit = when (period) {
        AlertaPeriod.HOY    -> today
        AlertaPeriod.SEMANA -> today.minusDays(7)
        AlertaPeriod.MES    -> today.minusDays(30)
    }
    return filter { alerta ->
        val date = runCatching {
            LocalDateTime.parse(alerta.fechaGeneracion, backendDateTimeFormatter).toLocalDate()
        }.getOrNull()
        when {
            date == null -> period == AlertaPeriod.MES
            period == AlertaPeriod.HOY -> date == today
            else -> !date.isBefore(limit)
        }
    }
}
