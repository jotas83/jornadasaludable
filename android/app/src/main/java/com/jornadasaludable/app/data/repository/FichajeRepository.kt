package com.jornadasaludable.app.data.repository

import com.google.gson.Gson
import com.jornadasaludable.app.data.api.ApiService
import com.jornadasaludable.app.data.api.dto.FichajeCreateRequest
import com.jornadasaludable.app.data.api.dto.FichajeCreateResponse
import com.jornadasaludable.app.data.api.dto.FichajeDto
import com.jornadasaludable.app.data.api.dto.FichajesIndexResponse
import com.jornadasaludable.app.data.api.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FichajeRepository @Inject constructor(
    private val api: ApiService,
    private val gson: Gson,
) {
    /** Fichajes de un día concreto (YYYY-MM-DD). Util para "historial de hoy". */
    suspend fun fichajesOfDay(date: String): Result<List<FichajeDto>> =
        safeApiCall(gson) {
            api.fichajesIndex(mapOf(
                "fecha_inicio" to date,
                "fecha_fin"    to date,
                "limit"        to "100",
            ))
        }.map { it.items }

    /** Crea un fichaje (ENTRADA o SALIDA). El UUID se genera client-side para idempotencia. */
    suspend fun crearFichaje(req: FichajeCreateRequest): Result<FichajeCreateResponse> =
        safeApiCall(gson) { api.fichajeCreate(req) }
}
