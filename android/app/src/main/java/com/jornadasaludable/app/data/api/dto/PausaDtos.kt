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
    /** BOCADILLO / COMIDA / DESCANSO_LEGAL / OTROS */
    val tipo: String,
    val inicio: String,
    /** Null si la pausa sigue abierta. */
    val fin: String?,
    @SerializedName("duracion_min") val duracionMin: Int,
    val latitud: Double?,
    val longitud: Double?,
    @SerializedName("computa_jornada") val computaJornada: Boolean,
)

/**
 * POST /pausas — único endpoint, despacha por `accion`.
 *   - INICIO: requiere `inicio` (ISO 8601) y opcionalmente `uuid` (idempotencia).
 *   - FIN:    requiere `fin`     (ISO 8601). Si se envía `uuid` y existe, ese
 *             es el target; si no, fallback a "última pausa abierta del mismo
 *             tipo en la jornada de la fecha del fin".
 *
 * Gson omite nulls por defecto (sin .serializeNulls()), así que solo el campo
 * que aplique a la acción viaja en el body.
 */
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
    /** Solo presente en INICIO; null en FIN. */
    val jornada: PausaJornadaInfo? = null,
    /** True si el backend detectó dup por uuid (no se hizo nada nuevo). */
    val idempotent: Boolean? = null,
)

data class PausaJornadaInfo(
    val uuid: String,
)
