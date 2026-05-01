package com.jornadasaludable.app.ui.alertas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jornadasaludable.app.data.repository.AlertaRepository
import com.jornadasaludable.app.data.repository.BurnoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel compartido por AlertasFragment y sus 3 sub-fragmentos
 * (Hoy/Semana/Mes). Los hijos lo obtienen vía:
 *   private val parentVM: AlertasViewModel by viewModels({ requireParentFragment() })
 *
 * Una sola fuente de verdad para alertas + burnout. Las pestañas filtran
 * client-side por período.
 */
@HiltViewModel
class AlertasViewModel @Inject constructor(
    private val alertaRepository:  AlertaRepository,
    private val burnoutRepository: BurnoutRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        AlertasUiState(
            burnout = Loadable.Loading,
            alertas = Loadable.Loading,
        )
    )
    val state: StateFlow<AlertasUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(burnout = Loadable.Loading, alertas = Loadable.Loading) }
            val burnoutJob = async { burnoutRepository.load() }
            val alertasJob = async { alertaRepository.listAll() }

            val burnout = burnoutJob.await()
                .map { it.actual }
                .fold(
                    onSuccess = { Loadable.Ready(it) },
                    onFailure = { Loadable.Error(it.message ?: "Error cargando burnout.") },
                )
            val alertas = alertasJob.await().fold(
                onSuccess = { Loadable.Ready(it) },
                onFailure = { Loadable.Error(it.message ?: "Error cargando alertas.") },
            )
            _state.update { it.copy(burnout = burnout, alertas = alertas) }
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
