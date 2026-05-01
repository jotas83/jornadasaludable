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
    /**
     * Devuelve la primera alerta sin leer del usuario, o null si no hay ninguna.
     * Para el Dashboard solo necesitamos saber si HAY alguna; un solo elemento
     * basta y minimiza payload.
     */
    suspend fun unreadFirst(): Result<AlertaDto?> {
        return safeApiCall(gson) {
            api.alertasIndex(mapOf("leida" to "0", "limit" to "1"))
        }.map { it.items.firstOrNull() }
    }

    /** Listado completo (sin filtro de leida) para el módulo Alertas. */
    suspend fun listAll(limit: Int = 200): Result<List<AlertaDto>> =
        safeApiCall(gson) {
            api.alertasIndex(mapOf("limit" to limit.toString()))
        }.map { it.items }

    /** Marca una alerta como leída. Devuelve la versión actualizada. */
    suspend fun marcarLeida(uuid: String): Result<AlertaDto> =
        safeApiCall(gson) { api.alertasMarcarLeida(uuid) }

    /**
     * Dispara la regeneración de alertas en el backend (re-evalúa umbrales
     * sobre las jornadas más recientes). Devuelve las que han pasado el
     * dedup — son las que el cliente debe notificar al usuario.
     */
    suspend fun generar(): Result<AlertasGenerarResponse> =
        safeApiCall(gson) { api.alertasGenerar() }
}
