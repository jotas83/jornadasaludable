package com.jornadasaludable.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jornadasaludable.app.data.repository.AlertaRepository
import com.jornadasaludable.app.data.repository.JornadaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DashboardTabViewModel @Inject constructor(
    private val jornadaRepository: JornadaRepository,
    private val alertaRepository: AlertaRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<DashboardTabUiState>(DashboardTabUiState.Loading)
    val state: StateFlow<DashboardTabUiState> = _state.asStateFlow()

    init { load() }

    /**
     * Carga en paralelo: resumen agregado, jornadas del mes (para encontrar
     * la de hoy) y la primera alerta sin leer. La alerta es no-crítica:
     * si falla, mostramos los otros dos bloques sin bloquear la pantalla.
     */
    fun load() {
        viewModelScope.launch {
            _state.value = DashboardTabUiState.Loading

            val resumenJob  = async { jornadaRepository.resumen() }
            val mesJob      = async { jornadaRepository.jornadasOfMonth(currentYearMonth()) }
            val alertaJob   = async { alertaRepository.unreadFirst() }

            val resumen = resumenJob.await().getOrElse {
                _state.value = DashboardTabUiState.Error(it.message ?: "Error cargando resumen.")
                return@launch
            }
            val mes = mesJob.await().getOrElse {
                _state.value = DashboardTabUiState.Error(it.message ?: "Error cargando jornadas.")
                return@launch
            }
            val alerta = alertaJob.await().getOrNull()

            val hoy = LocalDate.now().toString()  // YYYY-MM-DD
            val jornadaHoy = mes.items.firstOrNull { it.fecha == hoy }

            _state.value = DashboardTabUiState.Success(
                jornadaHoy    = jornadaHoy,
                resumen       = resumen,
                alertaSinLeer = alerta,
            )
        }
    }

    private fun currentYearMonth(): String =
        LocalDate.now().let { "%04d-%02d".format(it.year, it.monthValue) }
}
