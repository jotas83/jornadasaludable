package com.jornadasaludable.app.data.api

import com.google.gson.Gson
import com.google.gson.JsonObject
import retrofit2.Response
import java.io.IOException

/**
 * Wrapper común para repositorios: ejecuta un call de Retrofit que devuelve
 * `ApiEnvelope<T>` y traduce el resultado a `Result<T>`. Mapea:
 *
 *   - 2xx con `data` poblado          → Result.success(data)
 *   - 2xx con body vacío              → Result.failure(IOException)
 *   - 4xx/5xx con `error.message`     → Result.failure(message)
 *   - 4xx/5xx sin error parseable     → Result.failure(defaultHttpMessage)
 *   - IOException (sin red, timeout)  → Result.failure(IOException con mensaje legible)
 */
internal suspend fun <T> safeApiCall(
    gson: Gson,
    call: suspend () -> Response<ApiEnvelope<T>>,
): Result<T> {
    return try {
        val resp = call()
        if (resp.isSuccessful) {
            val data = resp.body()?.data
                ?: return Result.failure(IOException("Respuesta sin datos."))
            Result.success(data)
        } else {
            val msg = parseErrorMessage(gson, resp.errorBody()?.string())
                ?: defaultHttpMessage(resp.code())
            Result.failure(Exception(msg))
        }
    } catch (e: IOException) {
        Result.failure(IOException("Sin conexión: ${e.message ?: "no se pudo alcanzar el servidor."}"))
    } catch (e: Exception) {
        Result.failure(e)
    }
}

private fun parseErrorMessage(gson: Gson, body: String?): String? {
    if (body.isNullOrBlank()) return null
    return runCatching {
        val obj = gson.fromJson(body, JsonObject::class.java)
        obj?.getAsJsonObject("error")?.get("message")?.asString
    }.getOrNull()
}

private fun defaultHttpMessage(code: Int): String = when (code) {
    401 -> "Sesión caducada. Vuelve a iniciar sesión."
    403 -> "No tienes permiso para acceder a este recurso."
    404 -> "No encontrado."
    422 -> "Datos no válidos."
    in 500..599 -> "Error del servidor ($code). Inténtalo más tarde."
    else -> "Error HTTP $code"
}
