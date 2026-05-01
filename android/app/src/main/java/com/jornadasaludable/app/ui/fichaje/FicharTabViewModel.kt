package com.jornadasaludable.app.ui.fichaje

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jornadasaludable.app.data.api.dto.FichajeCreateRequest
import com.jornadasaludable.app.data.repository.FichajeRepository
import com.jornadasaludable.app.data.util.LocationProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class FicharTabViewModel @Inject constructor(
    private val fichajeRepository: FichajeRepository,
    private val locationProvider:  LocationProvider,
) : ViewModel() {

    private val _state = MutableStateFlow<FicharTabUiState>(FicharTabUiState.Loading)
    val state: StateFlow<FicharTabUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = FicharTabUiState.Loading
            val today = LocalDate.now().toString()
            val historial = fichajeRepository.fichajesOfDay(today).getOrElse {
                _state.value = FicharTabUiState.Error(it.message ?: "Error cargando fichajes.")
                return@launch
            }
            // Ordenar cronológicamente — el endpoint los devuelve DESC, queremos ASC
            // para que la lógica del último sea el último elemento.
            val cronologico = historial.sortedBy { it.timestampEvento }
            val ultimo = cronologico.lastOrNull()
            val estado = when (ultimo?.tipo) {
                "ENTRADA" -> JornadaEstado.TRABAJANDO
                else      -> JornadaEstado.IDLE
            }

            _state.value = FicharTabUiState.Ready(
                jornadaEstado = estado,
                historial     = cronologico,
                gps           = readGpsStatus(),
            )
        }
    }

    fun onPermissionResult() {
        // El estado del GPS puede haber cambiado; refrescamos solo ese bloque.
        val current = _state.value
        if (current is FicharTabUiState.Ready) {
            _state.value = current.copy(gps = readGpsStatus())
        }
    }

    fun ficharEntrada() = doFichaje("ENTRADA")
    fun ficharSalida()  = doFichaje("SALIDA")

    fun consumeMessage() {
        _state.update { state ->
            if (state is FicharTabUiState.Ready) state.copy(transientMessage = null) else state
        }
    }

    private fun doFichaje(tipo: String) {
        val current = _state.value
        if (current !is FicharTabUiState.Ready || current.submitting) return

        viewModelScope.launch {
            _state.value = current.copy(submitting = true, transientMessage = null)

            val location = locationProvider.requestSingleUpdate(timeoutMs = 4_000L)
            val now = OffsetDateTime.now(ZoneId.systemDefault())

            val req = FichajeCreateRequest(
                tipo            = tipo,
                timestampEvento = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                uuid            = UUID.randomUUID().toString(),  // idempotencia
                latitud         = location?.latitude,
                longitud        = location?.longitude,
                precisionGpsM   = location?.accuracy?.toDouble(),
                metodo          = "MANUAL",
            )

            fichajeRepository.crearFichaje(req)
                .onSuccess {
                    val msg = if (it.idempotent == true) {
                        "Fichaje ya registrado (idempotente)."
                    } else {
                        "Fichaje $tipo registrado."
                    }
                    _state.value = current.copy(transientMessage = msg, submitting = false)
                    refresh()
                }
                .onFailure { e ->
                    _state.value = current.copy(
                        transientMessage = "Error: ${e.message ?: "fichaje no registrado."}",
                        submitting       = false,
                    )
                }
        }
    }

    private fun readGpsStatus(): GpsStatus {
        val perm = locationProvider.hasPermission()
        val gps  = locationProvider.isGpsEnabled()
        val net  = locationProvider.isNetworkEnabled()
        val last = if (perm) locationProvider.lastKnownLocation() else null
        return GpsStatus(
            hasPermission  = perm,
            gpsEnabled     = gps,
            networkEnabled = net,
            lastFix        = last?.let { "%.5f, %.5f".format(it.latitude, it.longitude) },
        )
    }
}
