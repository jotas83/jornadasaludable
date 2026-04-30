package com.jornadasaludable.app.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jornadasaludable.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun login(identifier: String, password: String) {
        if (identifier.isBlank() || password.isBlank()) {
            _state.value = LoginUiState.Error("Email/NIF y contraseña son obligatorios.")
            return
        }

        viewModelScope.launch {
            _state.value = LoginUiState.Loading
            authRepository.login(identifier.trim(), password)
                .onSuccess { user -> _state.value = LoginUiState.Success(user) }
                .onFailure { e ->
                    _state.value = LoginUiState.Error(e.message ?: "Error desconocido")
                }
        }
    }

    /** Tras navegar a home, reseteamos para que un back-stack pop al login no se quede en Success. */
    fun reset() {
        _state.value = LoginUiState.Idle
    }
}
