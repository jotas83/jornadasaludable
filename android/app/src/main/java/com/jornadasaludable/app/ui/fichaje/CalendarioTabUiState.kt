package com.jornadasaludable.app.ui.fichaje

import com.jornadasaludable.app.data.api.dto.JornadaListItemDto
import java.time.YearMonth

sealed interface CalendarioTabUiState {
    data object Loading : CalendarioTabUiState
    data class Error(val message: String) : CalendarioTabUiState
    data class Ready(
        val mes: YearMonth,
        /** Mapa fecha (YYYY-MM-DD) → jornada. Días sin jornada NO están en el mapa. */
        val jornadasPorFecha: Map<String, JornadaListItemDto>,
    ) : CalendarioTabUiState
}
