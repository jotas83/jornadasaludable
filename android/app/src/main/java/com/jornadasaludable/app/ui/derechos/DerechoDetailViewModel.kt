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
class DerechoDetailViewModel @Inject constructor(
    private val derechoRepository: DerechoRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val codigo: String = savedStateHandle.get<String>("codigo")
        ?: throw IllegalArgumentException("Falta argumento 'codigo'")

    private val _state = MutableStateFlow<DerechoDetailUiState>(DerechoDetailUiState.Loading)
    val state: StateFlow<DerechoDetailUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = DerechoDetailUiState.Loading
            derechoRepository.show(codigo)
                .onSuccess { _state.value = DerechoDetailUiState.Success(it) }
                .onFailure { _state.value = DerechoDetailUiState.Error(it.message ?: "Error.") }
        }
    }
}
