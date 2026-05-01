package com.jornadasaludable.app.ui.estadisticas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jornadasaludable.app.data.repository.JornadaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResumenTabViewModel @Inject constructor(
    private val jornadaRepository: JornadaRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ResumenTabUiState>(ResumenTabUiState.Loading)
    val state: StateFlow<ResumenTabUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = ResumenTabUiState.Loading
            jornadaRepository.resumen()
                .onSuccess { _state.value = ResumenTabUiState.Success(it) }
                .onFailure { _state.value = ResumenTabUiState.Error(it.message ?: "Error.") }
        }
    }
}
