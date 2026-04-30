package com.jornadasaludable.app.data.api

import com.google.gson.annotations.SerializedName

/**
 * DTOs mínimos para el flow de autenticación. El resto de endpoints usa
 * `Map<String, Any?>` (request) y `JsonObject` (response) por ahora — se
 * irán tipando a medida que se construyan las pantallas.
 */

// =============================================================================
//  Auth
// =============================================================================

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

// =============================================================================
//  Envoltorio de respuesta uniforme
// =============================================================================

/**
 * Sobre genérico que la API devuelve en éxito:
 *   { "data": { ... } }
 *
 * Y en error:
 *   { "error": { "code": "...", "message": "...", "details": {...} } }
 */
data class ApiEnvelope<T>(
    val data: T? = null,
    val error: ApiError? = null,
)

data class ApiError(
    val code: String,
    val message: String,
    val details: Map<String, Any?>? = null,
)
