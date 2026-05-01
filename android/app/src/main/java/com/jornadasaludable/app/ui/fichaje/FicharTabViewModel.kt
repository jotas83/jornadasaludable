package com.jornadasaludable.app.ui.fichaje

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jornadasaludable.app.data.api.dto.FichajeCreateRequest
import com.jornadasaludable.app.data.api.dto.PausaDto
import com.jornadasaludable.app.data.notifications.AlertaNotificationService
import com.jornadasaludable.app.data.repository.AlertaRepository
import com.jornadasaludable.app.data.repository.FichajeOutcome
import com.jornadasaludable.app.data.repository.FichajeRepository
import com.jornadasaludable.app.data.repository.OfflineFichajeRepository
import com.jornadasaludable.app.data.repository.PausaRepository
import com.jornadasaludable.app.data.util.LocationProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class FicharTabViewModel @Inject constructor(
    private val fichajeRepository:        FichajeRepository,        // listado de hoy (lectura)
    private val offlineFichajeRepository: OfflineFichajeRepository, // creación offline-first
    private val pausaRepository:          PausaRepository,
    private val alertaRepository:         AlertaRepository,
    private val notificationService:      AlertaNotificationService,
    private val locationProvider:         LocationProvider,
) : ViewModel() {

    private val _state = MutableStateFlow<FicharTabUiState>(FicharTabUiState.Loading)
    val state: StateFlow<FicharTabUiState> = _state.asStateFlow()

    init {
        refresh()
        // El badge de pendientes offline se actualiza en cuanto el SyncWorker
        // borra filas o el usuario encola un fichaje nuevo.
        viewModelScope.launch {
            offlineFichajeRepository.pendingCountFlow().collectLatest { count ->
                _state.update {
                    if (it is FicharTabUiState.Ready) it.copy(pendingOffline = count) else it
                }
            }
        }
    }

    /**
     * Carga el estado del día. Si el endpoint de fichajes falla por IOException
     * (sin red, timeout) NO bloqueamos la UI: el usuario debe poder seguir
     * fichando — el fichaje irá a la cola offline. Marcamos `offlineMode=true`
     * para mostrarlo en la pantalla.
     *
     * Solo bloqueamos con Error si el fallo es semántico (4xx/5xx, JSON inválido…)
     * que un retry podría arreglar.
     */
    fun refresh() {
        viewModelScope.launch {
            _state.value = FicharTabUiState.Loading
            val today = LocalDate.now().toString()

            val historialResult = fichajeRepository.fichajesOfDay(today)
            val (historial, isOffline) = when {
                historialResult.isSuccess -> historialResult.getOrThrow() to false
                historialResult.exceptionOrNull() is IOException -> emptyList<com.jornadasaludable.app.data.api.dto.FichajeDto>() to true
                else -> {
                    _state.value = FicharTabUiState.Error(
                        historialResult.exceptionOrNull()?.message ?: "Error cargando fichajes."
                    )
                    return@launch
                }
            }
            val cronologico = historial.sortedBy { it.timestampEvento }
            val ultimo = cronologico.lastOrNull()

            // Detectar pausa abierta solo si estamos trabajando y hay red.
            val activePausa = if (ultimo?.tipo == "ENTRADA" && !isOffline) {
                detectActivePausa(today)
            } else null

            val estado = when {
                ultimo?.tipo == "ENTRADA" && activePausa != null -> JornadaEstado.EN_PAUSA
                ultimo?.tipo == "ENTRADA"                        -> JornadaEstado.TRABAJANDO
                else                                              -> JornadaEstado.IDLE
            }

            _state.value = FicharTabUiState.Ready(
                jornadaEstado  = estado,
                historial      = cronologico,
                gps            = readGpsStatus(),
                pendingOffline = offlineFichajeRepository.pendingCountOnce(),
                activePausa    = activePausa,
                offlineMode    = isOffline,
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

    /**
     * Una sola acción para INICIO/FIN — el botón muestra "Iniciar pausa" o
     * "Reanudar" según haya pausa abierta. La resolución del target en FIN se
     * hace por uuid (cargado en `activePausa`); si no, el backend cae al
     * fallback "última pausa abierta del mismo tipo".
     */
    fun togglePausa() {
        val current = _state.value
        if (current !is FicharTabUiState.Ready || current.submitting) return
        if (current.jornadaEstado == JornadaEstado.IDLE) return  // sin jornada no hay pausa

        viewModelScope.launch {
            _state.value = current.copy(submitting = true, transientMessage = null)

            val location = locationProvider.requestSingleUpdate(timeoutMs = 4_000L)
            val now = OffsetDateTime.now(ZoneId.systemDefault())
            val ts  = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

            val result = if (current.activePausa != null) {
                // FIN
                pausaRepository.finalizar(
                    timestampIso = ts,
                    tipo         = current.activePausa.tipo,
                    uuid         = current.activePausa.uuid,
                    latitud      = location?.latitude,
                    longitud     = location?.longitude,
                )
            } else {
                // INICIO
                pausaRepository.iniciar(
                    timestampIso = ts,
                    tipo         = "DESCANSO_LEGAL",
                    uuid         = UUID.randomUUID().toString(),
                    latitud      = location?.latitude,
                    longitud     = location?.longitude,
                )
            }

            result
                .onSuccess {
                    val msg = if (current.activePausa != null) "Pausa finalizada." else "Pausa iniciada."
                    _state.update { (it as? FicharTabUiState.Ready)?.copy(
                        transientMessage = msg, submitting = false,
                    ) ?: it }
                    refresh()
                }
                .onFailure { e ->
                    _state.update { (it as? FicharTabUiState.Ready)?.copy(
                        transientMessage = "Error pausa: ${e.message ?: "no se pudo registrar."}",
                        submitting       = false,
                    ) ?: it }
                }
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
                            triggerAlertasAndNotify()
                        }
                        is FichajeOutcome.QueuedOffline -> {
                            _state.update { (it as? FicharTabUiState.Ready)?.copy(
                                transientMessage = "Sin conexión. Guardado offline; se sincronizará al recuperar red.",
                                submitting = false,
                                // Computamos estado local manualmente: si era IDLE y fichamos ENTRADA,
                                // pasamos a TRABAJANDO; si era TRABAJANDO/EN_PAUSA y fichamos SALIDA, a IDLE.
                                jornadaEstado = nextEstadoLocal(it.jornadaEstado, tipo),
                            ) ?: it }
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

    private fun nextEstadoLocal(actual: JornadaEstado, tipoFichaje: String): JornadaEstado = when {
        tipoFichaje == "ENTRADA" && actual == JornadaEstado.IDLE       -> JornadaEstado.TRABAJANDO
        tipoFichaje == "SALIDA"  && actual != JornadaEstado.IDLE       -> JornadaEstado.IDLE
        else -> actual
    }

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

    /**
     * Busca una pausa con `fin = null` cuyo `inicio` sea de hoy. La consulta
     * se hace sin filtro de jornada (no tenemos el uuid de la jornada local) y
     * devuelve a lo sumo 20 — suficiente para detectar la abierta más reciente.
     */
    private suspend fun detectActivePausa(todayIsoDate: String): ActivePausa? {
        val pausas: List<PausaDto> = pausaRepository.list(limit = 20).getOrNull() ?: return null
        return pausas.firstOrNull { p ->
            p.fin == null && p.inicio.startsWith(todayIsoDate)
        }?.let { ActivePausa(uuid = it.uuid, tipo = it.tipo) }
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
