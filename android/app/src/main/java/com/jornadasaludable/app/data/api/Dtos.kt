package com.jornadasaludable.app.data.api

import com.google.gson.annotations.SerializedName

// DTOs base para autenticación y el sobre de respuesta común.

data class LoginRequest(
    val nif: String? = null,
    val email: String? = null,
    val password: String,
    @SerializedName("device_id") val deviceId: String? = null,
    @SerializedName("push_token") val pushToken: String? = null,
)

data class RefreshRequest(
    @SerializedName("refresh_token") val refreshToken: String,
)

data class AuthResponse(
    @SerializedName("access_token")  val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("token_type")    val tokenType: String,
    @SerializedName("expires_in")    val expiresIn: Long,
    val user: UserDto,
)

data class UserDto(
    val id: Long,
    val uuid: String,
    val nif: String,
    val nombre: String,
    val apellidos: String,
    val email: String?,
    val idioma: String?,
)

// Sobre genérico: { data } o { error }.
data class ApiEnvelope<T>(
    val data: T? = null,
    val error: ApiError? = null,
)

data class ApiError(
    val code: String,
    val message: String,
    val details: Map<String, Any?>? = null,
)
