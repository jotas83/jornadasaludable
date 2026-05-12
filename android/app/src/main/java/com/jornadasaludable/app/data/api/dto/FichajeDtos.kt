package com.jornadasaludable.app.data.api.dto

import com.google.gson.annotations.SerializedName

data class FichajesIndexResponse(
    val items: List<FichajeDto>,
    val limit: Int,
    val offset: Int,
)

data class FichajeDto(
    val uuid: String,
    @SerializedName("jornada_id")        val jornadaId: Long,
    val tipo: String,
    @SerializedName("timestamp_evento")  val timestampEvento: String,
    val latitud: Double?,
    val longitud: Double?,
    val metodo: String,
    @SerializedName("sync_status")       val syncStatus: String,
)

data class FichajeCreateRequest(
    val tipo: String,
    @SerializedName("timestamp_evento") val timestampEvento: String,
    // uuid opcional; si se envía sirve para idempotencia.
    val uuid: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    @SerializedName("precision_gps_m")  val precisionGpsM: Double? = null,
    @SerializedName("dentro_geofence")  val dentroGeofence: Boolean? = null,
    val metodo: String = "MANUAL",
    @SerializedName("device_id")        val deviceId: String? = null,
)

data class FichajeCreateResponse(
    val fichaje: FichajeDto,
    val jornada: JornadaListItemDto?,
    val idempotent: Boolean? = null,
)
