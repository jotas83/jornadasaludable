package com.jornadasaludable.app.data.api.dto

import com.google.gson.annotations.SerializedName

data class PausasIndexResponse(
    val items: List<PausaDto>,
    val limit: Int,
    val offset: Int,
)

data class PausaDto(
    val uuid: String,
    @SerializedName("jornada_id") val jornadaId: Long,
    val tipo: String,
    val inicio: String,
    val fin: String?,
    @SerializedName("duracion_min") val duracionMin: Int,
    val latitud: Double?,
    val longitud: Double?,
    @SerializedName("computa_jornada") val computaJornada: Boolean,
)

// accion = INICIO o FIN. En INICIO se manda `inicio`; en FIN se manda `fin`.
data class PausaCreateRequest(
    val accion: String,
    val tipo: String = "DESCANSO_LEGAL",
    val inicio: String? = null,
    val fin:    String? = null,
    val uuid: String? = null,
    val latitud:  Double? = null,
    val longitud: Double? = null,
)

data class PausaCreateResponse(
    val pausa: PausaDto,
    val jornada: PausaJornadaInfo? = null,
    val idempotent: Boolean? = null,
)

data class PausaJornadaInfo(
    val uuid: String,
)
