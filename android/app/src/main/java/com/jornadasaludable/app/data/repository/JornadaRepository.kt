package com.jornadasaludable.app.data.repository

import com.google.gson.Gson
import com.jornadasaludable.app.data.api.ApiService
import com.jornadasaludable.app.data.api.dto.JornadasIndexResponse
import com.jornadasaludable.app.data.api.dto.ResumenDto
import com.jornadasaludable.app.data.api.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JornadaRepository @Inject constructor(
    private val api: ApiService,
    private val gson: Gson,
) {
    /** Datos agregados semana/mes/año + horas_contrato. */
    suspend fun resumen(): Result<ResumenDto> =
        safeApiCall(gson) { api.jornadasResumen() }

    /**
     * Listado de jornadas del mes en formato YYYY-MM. Devuelve también
     * paginación (limit/offset) por si el front quiere más detalle.
     */
    suspend fun jornadasOfMonth(yearMonth: String): Result<JornadasIndexResponse> =
        safeApiCall(gson) { api.jornadasIndex(mapOf("mes" to yearMonth)) }
}
