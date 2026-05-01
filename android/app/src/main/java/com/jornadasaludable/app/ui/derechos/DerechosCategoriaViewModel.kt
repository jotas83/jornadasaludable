package com.jornadasaludable.app.ui.derechos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jornadasaludable.app.data.repository.DerechoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DerechosCategoriaViewModel @Inject constructor(
    private val derechoRepository: DerechoRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val codigoCategoria: String = savedStateHandle.get<String>("codigoCategoria")
        ?: throw IllegalArgumentException("Falta argumento codigoCategoria")
    private val nombreCategoria: String = savedStateHandle.get<String>("nombreCategoria") ?: ""

    private val _state = MutableStateFlow<DerechosCategoriaUiState>(DerechosCategoriaUiState.Loading)
    val state: StateFlow<DerechosCategoriaUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = DerechosCategoriaUiState.Loading
            derechoRepository.contenidosByCategoria(codigoCategoria)
                .onSuccess { _state.value = DerechosCategoriaUiState.Success(nombreCategoria, it) }
                .onFailure { _state.value = DerechosCategoriaUiState.Error(it.message ?: "Error.") }
        }
    }
}
