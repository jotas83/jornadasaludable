package com.jornadasaludable.app.ui.alertas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jornadasaludable.app.data.api.dto.SobrecargaEvaluacionDto
import com.jornadasaludable.app.data.repository.AlertaRepository
import com.jornadasaludable.app.data.repository.SobrecargaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// VM compartido por AlertasFragment y sus tres tabs (Hoy/Semana/Mes).
@HiltViewModel
class AlertasViewModel @Inject constructor(
    private val alertaRepository:     AlertaRepository,
    private val sobrecargaRepository: SobrecargaRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        AlertasUiState(
            sobrecarga = Loadable.Loading,
            alertas    = Loadable.Loading,
        )
    )
    val state: StateFlow<AlertasUiState> = _state.asStateFlow()

    init { load() }

    // generar() es la fuente principal de sobrecarga (la recalcula); GET /sobrecarga es fallback.
    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(sobrecarga = Loadable.Loading, alertas = Loadable.Loading) }

            val generarJob = async { alertaRepository.generar() }
            val alertasJob = async { alertaRepository.listAll() }

            val generarSobrecarga: SobrecargaEvaluacionDto? = generarJob.await().getOrNull()?.sobrecarga

            val historySobrecarga: SobrecargaEvaluacionDto? = if (generarSobrecarga == null) {
                sobrecargaRepository.load().getOrNull()?.actual
            } else null

            val effective = generarSobrecarga ?: historySobrecarga
            val sobrecargaState: Loadable<SobrecargaEvaluacionDto?> = Loadable.Ready(effective)

            val alertas = alertasJob.await().fold(
                onSuccess = { Loadable.Ready(it) },
                onFailure = { Loadable.Error(it.message ?: "Error cargando alertas.") },
            )
            _state.update { it.copy(sobrecarga = sobrecargaState, alertas = alertas) }
        }
    }

    fun marcarLeida(uuid: String) {
        viewModelScope.launch {
            _state.update { it.copy(markingUuid = uuid) }
            alertaRepository.marcarLeida(uuid)
                .onSuccess { actualizada ->
                    _state.update { current ->
                        val updatedList = (current.alertas as? Loadable.Ready)?.data
                            ?.map { if (it.uuid == uuid) actualizada else it }
                            ?: emptyList()
                        current.copy(
                            alertas = Loadable.Ready(updatedList),
                            markingUuid = null,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            markingUuid = null,
                            transientMessage = e.message ?: "Error marcando como leída.",
                        )
                    }
                }
        }
    }

    fun consumeMessage() {
        _state.update { it.copy(transientMessage = null) }
    }
}
