package com.jornadasaludable.app.ui.dashboard

import com.jornadasaludable.app.data.api.dto.EmpresaDto
import com.jornadasaludable.app.data.api.dto.PerfilDto

sealed interface PerfilTabUiState {
    data object Loading : PerfilTabUiState
    data class Error(val message: String) : PerfilTabUiState
    data class Success(
        val perfil:  PerfilDto,
        /** Null si el usuario no tiene contrato vigente (no es error). */
        val empresa: EmpresaDto?,
    ) : PerfilTabUiState
}
