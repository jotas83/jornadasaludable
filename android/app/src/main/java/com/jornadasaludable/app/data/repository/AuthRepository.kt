package com.jornadasaludable.app.data.repository

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.jornadasaludable.app.data.api.ApiService
import com.jornadasaludable.app.data.api.LoginRequest
import com.jornadasaludable.app.data.api.UserDto
import com.jornadasaludable.app.data.local.database.UserDao
import com.jornadasaludable.app.data.local.database.UserEntity
import com.jornadasaludable.app.data.local.preferences.TokenStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Punto único de entrada al flujo de autenticación. ViewModels llaman aquí,
 * no a la API ni al TokenStore directamente.
 *
 * Contrato: devuelve `Result<UserDto>` para que el caller pueda hacer
 * `.onSuccess { ... }.onFailure { ... }` sin try/catch en el ViewModel.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val api: ApiService,
    private val tokenStore: TokenStore,
    private val userDao: UserDao,
    private val gson: Gson,
) {

    /**
     * Detecta si el identificador es email (contiene '@') y lo manda al
     * backend en el campo apropiado. El backend acepta ambos por separado.
     */
    suspend fun login(identifier: String, password: String): Result<UserDto> {
        if (identifier.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Email/NIF y contraseña son obligatorios."))
        }

        return try {
            val isEmail = identifier.contains('@')
            val req = LoginRequest(
                nif      = if (!isEmail) identifier else null,
                email    = if (isEmail) identifier else null,
                password = password,
            )
            val resp = api.login(req)

            if (resp.isSuccessful) {
                val auth = resp.body()?.data
                    ?: return Result.failure(IOException("Respuesta sin datos."))

                tokenStore.saveTokens(auth.accessToken, auth.refreshToken)
                userDao.deleteAll()
                userDao.upsert(UserEntity.fromDto(auth.user))

                Result.success(auth.user)
            } else {
                Result.failure(Exception(parseErrorMessage(resp.errorBody()?.string()) ?: defaultHttpMessage(resp.code())))
            }
        } catch (e: IOException) {
            Result.failure(IOException("Sin conexión: ${e.message ?: "no se pudo alcanzar el servidor."}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        // Logout local siempre, aunque la llamada al servidor falle (offline o token expirado).
        runCatching { api.logout() }
        tokenStore.clear()
        userDao.deleteAll()
    }

    /** True si hay un access token persistido (no garantiza que sea válido). */
    val isAuthenticated: Flow<Boolean>
        get() = tokenStore.accessToken.map { !it.isNullOrBlank() }

    val currentUser: Flow<UserEntity?>
        get() = userDao.observeCurrent()

    // ---------------------------------------------------------------

    private fun parseErrorMessage(body: String?): String? {
        if (body.isNullOrBlank()) return null
        return runCatching {
            val obj = gson.fromJson(body, JsonObject::class.java)
            obj?.getAsJsonObject("error")?.get("message")?.asString
        }.getOrNull()
    }

    private fun defaultHttpMessage(code: Int): String = when (code) {
        401  -> "Credenciales inválidas."
        403  -> "Cuenta desactivada o sin permisos."
        422  -> "Datos enviados no válidos."
        in 500..599 -> "Error del servidor (${code}). Inténtalo más tarde."
        else -> "Error HTTP $code"
    }
}
