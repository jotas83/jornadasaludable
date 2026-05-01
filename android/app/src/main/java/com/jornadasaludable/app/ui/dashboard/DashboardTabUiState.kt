package com.jornadasaludable.app.ui.dashboard

import com.jornadasaludable.app.data.api.dto.AlertaDto
import com.jornadasaludable.app.data.api.dto.JornadaListItemDto
import com.jornadasaludable.app.data.api.dto.ResumenDto

sealed interface DashboardTabUiState {
    data object Loading : DashboardTabUiState
    data class Error(val message: String) : DashboardTabUiState
    data class Success(
        val jornadaHoy:    JornadaListItemDto?,
        val resumen:       ResumenDto,
        val alertaSinLeer: AlertaDto?,
    ) : DashboardTabUiState
}
