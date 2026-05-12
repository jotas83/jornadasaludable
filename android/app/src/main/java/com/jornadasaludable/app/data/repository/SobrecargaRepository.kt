package com.jornadasaludable.app.data.repository

import com.google.gson.Gson
import com.jornadasaludable.app.data.api.ApiService
import com.jornadasaludable.app.data.api.dto.SobrecargaResponse
import com.jornadasaludable.app.data.api.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SobrecargaRepository @Inject constructor(
    private val api: ApiService,
    private val gson: Gson,
) {
    suspend fun load(): Result<SobrecargaResponse> =
        safeApiCall(gson) { api.sobrecargaIndex(mapOf("limit" to "30")) }
}
