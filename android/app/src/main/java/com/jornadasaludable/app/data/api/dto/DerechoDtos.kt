package com.jornadasaludable.app.data.api.dto

import com.google.gson.annotations.SerializedName

/**
 * GET /derechos/categorias
 */
data class CategoriasResponse(
    val items: List<CategoriaDto>,
)

data class CategoriaDto(
    val codigo: String,
    val nombre: String,
    val descripcion: String?,
    val icono: String?,
    val orden: Int,
)

/**
 * GET /derechos/categorias/{codigo}/contenidos  y
 * GET /derechos/buscar — el backend devuelve { items: [...] } también para buscar.
 */
data class DerechosContenidosResponse(
    val items: List<DerechoListItemDto>,
)

data class DerechoBuscarResponse(
    val query: String,
    val items: List<DerechoListItemDto>,
)

data class DerechoListItemDto(
    val codigo: String,
    val titulo: String,
    @SerializedName("articulo_referencia") val articuloReferencia: String,
    val resumen: String,
    @SerializedName("url_boe")       val urlBoe: String?,
    @SerializedName("vigente_desde") val vigenteDesde: String?,
    val orden: Int?,
    /** Solo presente en /buscar (incluye el código de la categoría). */
    val categoria: String? = null,
)

/**
 * GET /derechos/{codigo} — detalle.
 */
data class DerechoDto(
    val codigo: String,
    val titulo: String,
    @SerializedName("articulo_referencia") val articuloReferencia: String,
    val resumen: String,
    /** Markdown completo. */
    val contenido: String,
    @SerializedName("palabras_clave") val palabrasClave: String?,
    @SerializedName("url_boe")        val urlBoe: String?,
    val idioma: String,
    val version: String,
    @SerializedName("vigente_desde")  val vigenteDesde: String,
    @SerializedName("vigente_hasta")  val vigenteHasta: String?,
    @SerializedName("consultas_count") val consultasCount: Int,
    @SerializedName("categoria_codigo") val categoriaCodigo: String,
    @SerializedName("categoria_nombre") val categoriaNombre: String,
)
