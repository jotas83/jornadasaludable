package com.jornadasaludable.app.data.repository

import com.google.gson.Gson
import com.jornadasaludable.app.data.api.ApiService
import com.jornadasaludable.app.data.api.dto.BurnoutResponse
import com.jornadasaludable.app.data.api.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BurnoutRepository @Inject constructor(
    private val api: ApiService,
    private val gson: Gson,
) {
    suspend fun load(): Result<BurnoutResponse> =
        safeApiCall(gson) { api.burnoutIndex(mapOf("limit" to "30")) }
}
