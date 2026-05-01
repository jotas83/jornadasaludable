package com.jornadasaludable.app.data.api.dto

import com.google.gson.annotations.SerializedName

/**
 * GET /usuarios/perfil → datos del trabajador autenticado.
 */
data class PerfilDto(
    val id: Long,
    val uuid: String,
    val nif: String,
    val nombre: String,
    val apellidos: String,
    val email: String?,
    val telefono: String?,
    val idioma: String?,
    @SerializedName("fecha_nacimiento") val fechaNacimiento: String?,
    val nacionalidad: String?,
    @SerializedName("last_login_at")    val lastLoginAt: String?,
    @SerializedName("last_sync_at")     val lastSyncAt: String?,
    @SerializedName("created_at")       val createdAt: String?,
)

/**
 * GET /usuarios/empresa → empresa actual derivada del contrato vigente.
 */
data class EmpresaDto(
    val id: Long,
    val cif: String,
    @SerializedName("razon_social")    val razonSocial: String,
    @SerializedName("nombre_comercial") val nombreComercial: String?,
    val direccion: String?,
    val cp: String?,
    val municipio: String?,
    val provincia: String?,
    val email: String?,
    val telefono: String?,
    @SerializedName("sector_codigo")   val sectorCodigo: String,
    @SerializedName("sector_nombre")   val sectorNombre: String,
    @SerializedName("contrato_tipo")   val contratoTipo: String,
    @SerializedName("jornada_tipo")    val jornadaTipo: String,
    @SerializedName("horas_semanales") val horasSemanales: Double,
    @SerializedName("contrato_desde")  val contratoDesde: String,
    @SerializedName("contrato_hasta")  val contratoHasta: String?,
)
