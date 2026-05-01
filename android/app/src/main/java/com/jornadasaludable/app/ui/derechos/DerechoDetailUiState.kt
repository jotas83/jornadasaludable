package com.jornadasaludable.app.ui.derechos

import com.jornadasaludable.app.data.api.dto.DerechoDto

sealed interface DerechoDetailUiState {
    data object Loading : DerechoDetailUiState
    data class Error(val message: String) : DerechoDetailUiState
    data class Success(val derecho: DerechoDto) : DerechoDetailUiState
}
