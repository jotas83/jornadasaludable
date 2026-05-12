package com.jornadasaludable.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jornadasaludable.app.data.repository.AuthRepository
import com.jornadasaludable.app.data.repository.UsuarioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PerfilTabViewModel @Inject constructor(
    private val usuarioRepository: UsuarioRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<PerfilTabUiState>(PerfilTabUiState.Loading)
    val state: StateFlow<PerfilTabUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = PerfilTabUiState.Loading

            val perfilJob  = async { usuarioRepository.perfil() }
            val empresaJob = async { usuarioRepository.empresa() }

            val perfil = perfilJob.await().getOrElse {
                _state.value = PerfilTabUiState.Error(it.message ?: "Error cargando perfil.")
                return@launch
            }
            // Si no hay contrato vigente, el endpoint devuelve 404; mostramos solo perfil.
            val empresa = empresaJob.await().getOrNull()

            _state.value = PerfilTabUiState.Success(perfil, empresa)
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onDone()
        }
    }
}
