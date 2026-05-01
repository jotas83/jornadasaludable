package com.jornadasaludable.app.ui.alertas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jornadasaludable.app.data.api.dto.BurnoutEvaluacionDto
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

    /**
     * Estrategia de carga del bloque burnout:
     *   1. POST /alertas/generar fuerza al backend a recalcular y persistir
     *      la evaluación. La respuesta incluye un campo `burnout` con los
     *      valores recién calculados — más fiable que GET /burnout, que puede
     *      devolver `actual=null` si la tabla histórica está vacía.
     *   2. GET /burnout como fallback (último histórico persistido).
     *   3. Display: lo más fresco de los dos. generar.burnout > actual > null.
     *
     * Si ambas calls fallan, mostramos error solo si TAMBIÉN la lista de
     * alertas falla; si solo falla burnout, tab del burnout queda en Ready
     * con null y la UI muestra "Sin datos suficientes".
     */
    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(burnout = Loadable.Loading, alertas = Loadable.Loading) }

            // Paralelo: generar (fuerza recálculo) + listAll (lista alertas).
            val generarJob = async { alertaRepository.generar() }
            val alertasJob = async { alertaRepository.listAll() }

            // generar() devuelve burnout fresco si tiene éxito
            val generarBurnout: BurnoutEvaluacionDto? = generarJob.await().getOrNull()?.burnout

            // GET /burnout como fallback — solo si generar no nos dio nada
            val historyBurnout: BurnoutEvaluacionDto? = if (generarBurnout == null) {
                burnoutRepository.load().getOrNull()?.actual
            } else null

            val effective = generarBurnout ?: historyBurnout
            val burnoutState: Loadable<BurnoutEvaluacionDto?> = Loadable.Ready(effective)

            val alertas = alertasJob.await().fold(
                onSuccess = { Loadable.Ready(it) },
                onFailure = { Loadable.Error(it.message ?: "Error cargando alertas.") },
            )
            _state.update { it.copy(burnout = burnoutState, alertas = alertas) }
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
