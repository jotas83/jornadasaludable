package com.jornadasaludable.app.data.repository

import com.google.gson.Gson
import com.jornadasaludable.app.data.api.ApiService
import com.jornadasaludable.app.data.api.dto.AlertaDto
import com.jornadasaludable.app.data.api.dto.AlertasGenerarResponse
import com.jornadasaludable.app.data.api.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertaRepository @Inject constructor(
    private val api: ApiService,
    private val gson: Gson,
) {
    // Primera alerta sin leer (basta una para el indicador del Dashboard).
    suspend fun unreadFirst(): Result<AlertaDto?> {
        return safeApiCall(gson) {
            api.alertasIndex(mapOf("leida" to "0", "limit" to "1"))
        }.map { it.items.firstOrNull() }
    }

    suspend fun listAll(limit: Int = 200): Result<List<AlertaDto>> =
        safeApiCall(gson) {
            api.alertasIndex(mapOf("limit" to limit.toString()))
        }.map { it.items }

    suspend fun marcarLeida(uuid: String): Result<AlertaDto> =
        safeApiCall(gson) { api.alertasMarcarLeida(uuid) }

    // Pide al backend que re-evalúe umbrales. Devuelve solo las nuevas tras dedup.
    suspend fun generar(): Result<AlertasGenerarResponse> =
        safeApiCall(gson) { api.alertasGenerar() }
}
