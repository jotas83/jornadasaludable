package com.jornadasaludable.app.ui.fichaje

import com.jornadasaludable.app.data.api.dto.FichajeDto

/**
 * Estado derivado del último fichaje del día — determina qué botón está
 * disponible.
 */
enum class JornadaEstado {
    /** Sin fichajes hoy o último fichaje fue SALIDA → puede fichar ENTRADA. */
    IDLE,
    /** Último fichaje fue ENTRADA → puede fichar SALIDA o iniciar pausa. */
    TRABAJANDO,
}

data class GpsStatus(
    val hasPermission: Boolean,
    val gpsEnabled:    Boolean,
    val networkEnabled: Boolean,
    val lastFix: String? = null,
)

sealed interface FicharTabUiState {
    data object Loading : FicharTabUiState
    data class Error(val message: String) : FicharTabUiState
    data class Ready(
        val jornadaEstado: JornadaEstado,
        val historial:     List<FichajeDto>,
        val gps:           GpsStatus,
        val submitting:    Boolean = false,
        /** Mensaje transitorio tras crear/error fichaje. Consume con consumeMessage(). */
        val transientMessage: String? = null,
        /** Nº de fichajes encolados en Room esperando red. */
        val pendingOffline: Int = 0,
    ) : FicharTabUiState
}
