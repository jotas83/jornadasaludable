package com.jornadasaludable.app.ui.alertas

import com.jornadasaludable.app.data.api.dto.AlertaDto
import com.jornadasaludable.app.data.api.dto.BurnoutEvaluacionDto

data class AlertasUiState(
    val burnout:    Loadable<BurnoutEvaluacionDto?>,
    val alertas:    Loadable<List<AlertaDto>>,
    val markingUuid: String? = null,        // mostrando spinner en el item que se marca
    val transientMessage: String? = null,
)

sealed interface Loadable<out T> {
    data object Loading : Loadable<Nothing>
    data class Error(val message: String) : Loadable<Nothing>
    data class Ready<T>(val data: T) : Loadable<T>
}
