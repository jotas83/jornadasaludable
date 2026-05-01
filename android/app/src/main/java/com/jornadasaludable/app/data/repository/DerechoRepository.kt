package com.jornadasaludable.app.data.repository

import com.google.gson.Gson
import com.jornadasaludable.app.data.api.ApiService
import com.jornadasaludable.app.data.api.dto.CategoriaDto
import com.jornadasaludable.app.data.api.dto.DerechoDto
import com.jornadasaludable.app.data.api.dto.DerechoListItemDto
import com.jornadasaludable.app.data.api.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DerechoRepository @Inject constructor(
    private val api: ApiService,
    private val gson: Gson,
) {
    suspend fun categorias(): Result<List<CategoriaDto>> =
        safeApiCall(gson) { api.derechosCategorias() }.map { it.items }

    suspend fun contenidosByCategoria(codigoCategoria: String): Result<List<DerechoListItemDto>> =
        safeApiCall(gson) { api.derechosContenidos(codigoCategoria) }.map { it.items }

    suspend fun show(codigo: String): Result<DerechoDto> =
        safeApiCall(gson) { api.derechosShow(codigo) }

    suspend fun buscar(query: String): Result<List<DerechoListItemDto>> =
        safeApiCall(gson) { api.derechosBuscar(query) }.map { it.items }
}
