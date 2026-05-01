package com.jornadasaludable.app.ui.derechos

import com.jornadasaludable.app.data.api.dto.DerechoListItemDto

sealed interface DerechosCategoriaUiState {
    data object Loading : DerechosCategoriaUiState
    data class Error(val message: String) : DerechosCategoriaUiState
    data class Success(
        val nombreCategoria: String,
        val derechos: List<DerechoListItemDto>,
    ) : DerechosCategoriaUiState
}
