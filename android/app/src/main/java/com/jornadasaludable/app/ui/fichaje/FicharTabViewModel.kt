package com.jornadasaludable.app.ui.fichaje

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jornadasaludable.app.data.api.dto.FichajeCreateRequest
import com.jornadasaludable.app.data.notifications.AlertaNotificationService
import com.jornadasaludable.app.data.repository.AlertaRepository
import com.jornadasaludable.app.data.repository.FichajeOutcome
import com.jornadasaludable.app.data.repository.FichajeRepository
import com.jornadasaludable.app.data.repository.OfflineFichajeRepository
import com.jornadasaludable.app.data.util.LocationProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
    private val fichajeRepository: FichajeRepository,                // listado de hoy (lectura)
    private val offlineFichajeRepository: OfflineFichajeRepository,  // creación offline-first
    private val alertaRepository: AlertaRepository,
    private val notificationService: AlertaNotificationService,
    private val locationProvider:  LocationProvider,
) : ViewModel() {

    private val _state = MutableStateFlow<FicharTabUiState>(FicharTabUiState.Loading)
    val state: StateFlow<FicharTabUiState> = _state.asStateFlow()

    init {
        refresh()
        // Observa el contador de pendientes offline; se actualiza al insertar
        // o al borrar tras sync con éxito.
        viewModelScope.launch {
            offlineFichajeRepository.pendingCountFlow().collectLatest { count ->
                _state.update {
                    if (it is FicharTabUiState.Ready) it.copy(pendingOffline = count) else it
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = FicharTabUiState.Loading
            val today = LocalDate.now().toString()
            val historial = fichajeRepository.fichajesOfDay(today).getOrElse {
                _state.value = FicharTabUiState.Error(it.message ?: "Error cargando fichajes.")
                return@launch
            }
            val cronologico = historial.sortedBy { it.timestampEvento }
            val ultimo = cronologico.lastOrNull()
            val estado = when (ultimo?.tipo) {
                "ENTRADA" -> JornadaEstado.TRABAJANDO
                else      -> JornadaEstado.IDLE
            }

            _state.value = FicharTabUiState.Ready(
                jornadaEstado  = estado,
                historial      = cronologico,
                gps            = readGpsStatus(),
                pendingOffline = offlineFichajeRepository.pendingCountOnce(),
            )
        }
    }

    fun onPermissionResult() {
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
                uuid            = UUID.randomUUID().toString(),
                latitud         = location?.latitude,
                longitud        = location?.longitude,
                precisionGpsM   = location?.accuracy?.toDouble(),
                metodo          = "MANUAL",
            )

            offlineFichajeRepository.crearFichaje(req)
                .onSuccess { outcome ->
                    when (outcome) {
                        is FichajeOutcome.Synced -> {
                            val msg = if (outcome.response.idempotent == true)
                                "Fichaje ya registrado." else "Fichaje $tipo registrado."
                            _state.update { (it as? FicharTabUiState.Ready)?.copy(
                                transientMessage = msg, submitting = false,
                            ) ?: it }
                            refresh()
                            // Re-evaluar alertas y notificar las nuevas. Best-effort.
                            triggerAlertasAndNotify()
                        }
                        is FichajeOutcome.QueuedOffline -> {
                            _state.update { (it as? FicharTabUiState.Ready)?.copy(
                                transientMessage = "Sin conexión. Guardado offline; se sincronizará al recuperar red.",
                                submitting = false,
                            ) ?: it }
                            // El refresh remoto fallaría; nos quedamos con el estado actual.
                        }
                    }
                }
                .onFailure { e ->
                    _state.update { (it as? FicharTabUiState.Ready)?.copy(
                        transientMessage = "Error: ${e.message ?: "fichaje no registrado."}",
                        submitting       = false,
                    ) ?: it }
                }
        }
    }

    /**
     * Tras un fichaje sincronizado, dispara la regeneración de alertas en
     * el backend. Las que pasan el dedup vienen en `alertas` — para cada una
     * lanzamos una notificación local. Si la llamada falla (offline, 5xx),
     * se ignora silenciosamente: las alertas se verán en el próximo refresh
     * del módulo Alertas.
     */
    private fun triggerAlertasAndNotify() {
        viewModelScope.launch {
            alertaRepository.generar().onSuccess { resp ->
                resp.alertas.forEach { alerta ->
                    notificationService.showAlerta(
                        notificationId = alerta.uuid.hashCode(),
                        tituloTipo     = alerta.tipoNombre,
                        mensaje        = alerta.mensaje,
                        baseLegal      = alerta.baseLegal,
                    )
                }
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
