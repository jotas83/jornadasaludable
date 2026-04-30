package com.jornadasaludable.app.ui.login

import com.jornadasaludable.app.data.api.UserDto

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Success(val user: UserDto) : LoginUiState
    data class Error(val message: String) : LoginUiState
}
