package com.jornadasaludable.app.ui.fichaje

import com.jornadasaludable.app.data.api.dto.FichajeDto

enum class JornadaEstado {
    IDLE, TRABAJANDO, EN_PAUSA,
}

data class GpsStatus(
    val hasPermission: Boolean,
    val gpsEnabled:    Boolean,
    val networkEnabled: Boolean,
    val lastFix: String? = null,
)

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
        val activePausa:   ActivePausa? = null,
        val offlineMode:   Boolean = false,
    ) : FicharTabUiState
}
