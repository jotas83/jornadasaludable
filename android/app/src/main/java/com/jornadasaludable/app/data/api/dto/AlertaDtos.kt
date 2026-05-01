package com.jornadasaludable.app.data.api.dto

import com.google.gson.annotations.SerializedName

/**
 * GET /alertas → listado paginado.
 */
data class AlertasIndexResponse(
    val items: List<AlertaDto>,
    val limit: Int,
    val offset: Int,
)

data class AlertaDto(
    val uuid: String,
    /** codigo del tipo: JORNADA_EXCEDIDA, DESCANSO_INSUFICIENTE, etc. */
    val tipo: String,
    @SerializedName("tipo_nombre")       val tipoNombre: String,
    /** INFORMATIVA / AVISO / GRAVE / CRITICA */
    val nivel: String,
    @SerializedName("base_legal")        val baseLegal: String?,
    val mensaje: String,
    @SerializedName("valor_detectado")   val valorDetectado: String?,
    @SerializedName("fecha_generacion")  val fechaGeneracion: String,
    val leida: Boolean,
    @SerializedName("leida_at")          val leidaAt: String?,
    @SerializedName("jornada_id")        val jornadaId: Long?,
)
