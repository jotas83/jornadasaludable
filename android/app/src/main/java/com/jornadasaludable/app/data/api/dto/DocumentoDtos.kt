package com.jornadasaludable.app.data.api.dto

import com.google.gson.annotations.SerializedName

data class DocumentosIndexResponse(
    val items: List<DocumentoDto>,
    val limit: Int,
    val offset: Int,
)

data class DocumentoDto(
    val uuid: String,
    /** REGISTRO_JORNADA_MENSUAL / RESUMEN_HORAS_EXTRA / SOLICITUD_VACACIONES / CERTIFICADO_DERECHOS / OTROS */
    val tipo: String,
    @SerializedName("periodo_desde") val periodoDesde: String?,
    @SerializedName("periodo_hasta") val periodoHasta: String?,
    @SerializedName("nombre_fichero") val nombreFichero: String,
    @SerializedName("tamano_bytes")  val tamanoBytes: Long?,
    @SerializedName("hash_sha256")   val hashSha256: String?,
    /** Backend devuelve TINYINT(1) sin cast a bool: 0/1 como número. */
    val firmado: Int,
    val descargado: Int,
    @SerializedName("descargado_at") val descargadoAt: String?,
    @SerializedName("created_at")    val createdAt: String,
) {
    val esFirmado:    Boolean get() = firmado    != 0
    val esDescargado: Boolean get() = descargado != 0
}

/**
 * POST /documentos/generar — para REGISTRO_JORNADA_MENSUAL el backend
 * espera `mes` en formato YYYY-MM.
 */
data class DocumentoCreateRequest(
    val tipo: String = "REGISTRO_JORNADA_MENSUAL",
    val mes: String? = null,
    @SerializedName("periodo_desde") val periodoDesde: String? = null,
    @SerializedName("periodo_hasta") val periodoHasta: String? = null,
)

data class DocumentoCreateResponse(
    val uuid: String,
    val tipo: String,
    @SerializedName("periodo_desde") val periodoDesde: String?,
    @SerializedName("periodo_hasta") val periodoHasta: String?,
    @SerializedName("nombre_fichero") val nombreFichero: String,
    @SerializedName("tamano_bytes")  val tamanoBytes: Long,
    @SerializedName("hash_sha256")   val hashSha256: String,
)
