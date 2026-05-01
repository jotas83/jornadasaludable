package com.jornadasaludable.app.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jornadasaludable.app.data.api.dto.FichajeCreateRequest

/**
 * Cola local de fichajes pendientes de sincronizar. El ciclo es:
 *
 *   crearFichaje() sin red  →  insert PENDING (uuid client-side, idempotencia)
 *   SyncWorker (network OK) →  POST /fichajes/sync   →  delete row al confirmarse
 *   API rechaza con 4xx     →  marcamos FAILED + lastError, ya no se reintenta
 *
 * El UUID del fichaje es la primary key (mismo que el del backend) — así un
 * reintento del SyncWorker es idempotente: si dos workers compiten, ambos
 * envían el mismo uuid y el backend responde `idempotent: true`.
 */
@Entity(tableName = "fichajes_pending")
data class FichajeEntity(
    @PrimaryKey val uuid: String,
    /** ENTRADA o SALIDA */
    val tipo: String,
    /** ISO 8601 con offset de la zona del cliente */
    val timestampEvento: String,
    val latitud: Double?,
    val longitud: Double?,
    val precisionGpsM: Double?,
    val dentroGeofence: Boolean?,
    /** MANUAL / AUTO_GEOFENCE / NFC / QR */
    val metodo: String,
    val deviceId: String?,
    /** PENDING / FAILED — los SYNCED se borran en lugar de quedarse aquí. */
    val syncStatus: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastError: String? = null,
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_FAILED  = "FAILED"

        fun fromRequest(req: FichajeCreateRequest): FichajeEntity = FichajeEntity(
            uuid            = req.uuid ?: throw IllegalArgumentException("uuid obligatorio para offline"),
            tipo            = req.tipo,
            timestampEvento = req.timestampEvento,
            latitud         = req.latitud,
            longitud        = req.longitud,
            precisionGpsM   = req.precisionGpsM,
            dentroGeofence  = req.dentroGeofence,
            metodo          = req.metodo,
            deviceId        = req.deviceId,
            syncStatus      = STATUS_PENDING,
        )
    }
}
