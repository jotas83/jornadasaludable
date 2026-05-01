package com.jornadasaludable.app.ui.estadisticas

import com.jornadasaludable.app.data.api.dto.ResumenDto

sealed interface ResumenTabUiState {
    data object Loading : ResumenTabUiState
    data class Error(val message: String) : ResumenTabUiState
    data class Success(val resumen: ResumenDto) : ResumenTabUiState
}
