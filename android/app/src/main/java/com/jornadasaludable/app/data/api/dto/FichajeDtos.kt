package com.jornadasaludable.app.data.api.dto

import com.google.gson.annotations.SerializedName

/**
 * GET /fichajes — listado paginado.
 */
data class FichajesIndexResponse(
    val items: List<FichajeDto>,
    val limit: Int,
    val offset: Int,
)

data class FichajeDto(
    val uuid: String,
    @SerializedName("jornada_id")        val jornadaId: Long,
    /** ENTRADA o SALIDA. */
    val tipo: String,
    @SerializedName("timestamp_evento")  val timestampEvento: String,
    val latitud: Double?,
    val longitud: Double?,
    /** MANUAL / AUTO_GEOFENCE / NFC / QR */
    val metodo: String,
    @SerializedName("sync_status")       val syncStatus: String,
)

/**
 * POST /fichajes (cuerpo).
 */
data class FichajeCreateRequest(
    /** ENTRADA o SALIDA. */
    val tipo: String,
    @SerializedName("timestamp_evento") val timestampEvento: String,
    /** Si null, el servidor genera uno; si se envía, sirve para idempotencia. */
    val uuid: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    @SerializedName("precision_gps_m")  val precisionGpsM: Double? = null,
    @SerializedName("dentro_geofence")  val dentroGeofence: Boolean? = null,
    val metodo: String = "MANUAL",
    @SerializedName("device_id")        val deviceId: String? = null,
)

/**
 * POST /fichajes (respuesta). Cuando el uuid coincide con uno ya creado,
 * el backend devuelve `idempotent=true` con la fila existente.
 */
data class FichajeCreateResponse(
    val fichaje: FichajeDto,
    val jornada: JornadaListItemDto?,
    val idempotent: Boolean? = null,
)
