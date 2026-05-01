package com.jornadasaludable.app.data.repository

import com.google.gson.Gson
import com.jornadasaludable.app.data.api.ApiService
import com.jornadasaludable.app.data.api.dto.DocumentoCreateRequest
import com.jornadasaludable.app.data.api.dto.DocumentoCreateResponse
import com.jornadasaludable.app.data.api.dto.DocumentoDto
import com.jornadasaludable.app.data.api.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentoRepository @Inject constructor(
    private val api: ApiService,
    private val gson: Gson,
) {
    suspend fun list(): Result<List<DocumentoDto>> =
        safeApiCall(gson) {
            api.documentosIndex(mapOf("limit" to "50"))
        }.map { resp -> resp.items }

    suspend fun generarMensual(mes: String): Result<DocumentoCreateResponse> =
        safeApiCall(gson) {
            api.documentosGenerar(
                DocumentoCreateRequest(tipo = "REGISTRO_JORNADA_MENSUAL", mes = mes)
            )
        }
}
