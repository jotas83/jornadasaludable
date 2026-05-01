package com.jornadasaludable.app.data.repository

import com.google.gson.Gson
import com.jornadasaludable.app.data.api.ApiService
import com.jornadasaludable.app.data.api.dto.PausaCreateRequest
import com.jornadasaludable.app.data.api.dto.PausaCreateResponse
import com.jornadasaludable.app.data.api.dto.PausaDto
import com.jornadasaludable.app.data.api.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PausaRepository @Inject constructor(
    private val api: ApiService,
    private val gson: Gson,
) {
    /** Lista pausas del usuario; opcionalmente filtra por jornada. */
    suspend fun list(jornadaUuid: String? = null, limit: Int = 50): Result<List<PausaDto>> {
        val params = mutableMapOf("limit" to limit.toString())
        if (jornadaUuid != null) params["jornada_uuid"] = jornadaUuid
        return safeApiCall(gson) { api.pausasIndex(params) }.map { it.items }
    }

    suspend fun iniciar(
        timestampIso: String,
        tipo: String = "DESCANSO_LEGAL",
        uuid: String,
        latitud: Double?,
        longitud: Double?,
    ): Result<PausaCreateResponse> = safeApiCall(gson) {
        api.pausaCreate(PausaCreateRequest(
            accion   = "INICIO",
            tipo     = tipo,
            inicio   = timestampIso,
            uuid     = uuid,
            latitud  = latitud,
            longitud = longitud,
        ))
    }

    suspend fun finalizar(
        timestampIso: String,
        tipo: String,
        uuid: String?,
        latitud: Double?,
        longitud: Double?,
    ): Result<PausaCreateResponse> = safeApiCall(gson) {
        api.pausaCreate(PausaCreateRequest(
            accion   = "FIN",
            tipo     = tipo,
            fin      = timestampIso,
            uuid     = uuid,
            latitud  = latitud,
            longitud = longitud,
        ))
    }
}
