package com.jornadasaludable.app.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jornadasaludable.app.data.api.dto.FichajeCreateRequest

// Cola offline. El uuid es el mismo que en el backend para que el sync sea idempotente.
@Entity(tableName = "fichajes_pending")
data class FichajeEntity(
    @PrimaryKey val uuid: String,
    val tipo: String,
    val timestampEvento: String,
    val latitud: Double?,
    val longitud: Double?,
    val precisionGpsM: Double?,
    val dentroGeofence: Boolean?,
    val metodo: String,
    val deviceId: String?,
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
