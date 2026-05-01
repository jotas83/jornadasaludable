package com.jornadasaludable.app.ui.fichaje

import com.jornadasaludable.app.data.api.dto.FichajeDto

/**
 * Estado derivado del último fichaje del día + presencia de pausa abierta.
 *
 *   IDLE       → sin entrada o último fichaje fue SALIDA (puede fichar ENTRADA)
 *   TRABAJANDO → entrada sin salida, sin pausa abierta (puede SALIDA o INICIAR PAUSA)
 *   EN_PAUSA   → entrada sin salida + pausa abierta (puede SALIDA o REANUDAR pausa)
 */
enum class JornadaEstado {
    IDLE, TRABAJANDO, EN_PAUSA,
}

data class GpsStatus(
    val hasPermission: Boolean,
    val gpsEnabled:    Boolean,
    val networkEnabled: Boolean,
    val lastFix: String? = null,
)

/** Info necesaria para cerrar la pausa abierta (resolución por uuid + tipo). */
data class ActivePausa(
    val uuid: String,
    val tipo: String,
)

sealed interface FicharTabUiState {
    data object Loading : FicharTabUiState
    data class Error(val message: String) : FicharTabUiState
    data class Ready(
        val jornadaEstado: JornadaEstado,
        val historial:     List<FichajeDto>,
        val gps:           GpsStatus,
        val submitting:    Boolean = false,
        val transientMessage: String? = null,
        val pendingOffline: Int = 0,
        /** Null si no hay pausa abierta; valor cuando jornadaEstado=EN_PAUSA. */
        val activePausa:   ActivePausa? = null,
        /** True si la última carga remota falló por red; UI puede avisar. */
        val offlineMode:   Boolean = false,
    ) : FicharTabUiState
}
