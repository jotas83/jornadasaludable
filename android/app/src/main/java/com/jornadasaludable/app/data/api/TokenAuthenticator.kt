package com.jornadasaludable.app.data.api

import android.util.Log
import com.jornadasaludable.app.data.local.preferences.TokenStore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * OkHttp Authenticator que resuelve 401 reactivamente. Patrón estándar:
 *
 *   1. Petición con access token → backend responde 401.
 *   2. authenticate() es invocado por OkHttp en thread de red.
 *   3. POST /auth/refresh con el refresh token guardado.
 *   4. Si éxito → guardamos los nuevos tokens y devolvemos la request original
 *      con el nuevo Bearer; OkHttp la reintenta automáticamente.
 *   5. Si fallo (refresh expirado, sin red, etc.) → null → el 401 original
 *      llega al caller que decidirá (típicamente: cerrar sesión).
 *
 * Detalles importantes:
 *
 *   - Usamos `Provider<ApiService>` para romper la dependencia circular
 *     OkHttpClient → Authenticator → ApiService → Retrofit → OkHttpClient.
 *     El Provider se resuelve perezosamente la primera vez que se invoca.
 *
 *   - Skipeamos los endpoints `/auth/login`, `/auth/refresh`, `/auth/logout`:
 *     un 401 ahí significa credenciales/token inválidos; reintentar refresh
 *     no arregla nada y crearía un bucle infinito.
 *
 *   - Mutex para evitar race con múltiples 401 simultáneos. El primero
 *     refresca; los siguientes ven el token actualizado y reintentan con él
 *     sin volver a refrescar.
 *
 *   - Guard de profundidad (responseCount) por si OkHttp decide reintentar
 *     más de una vez por petición original — en ese caso, abortamos.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStore: TokenStore,
    private val apiServiceProvider: Provider<ApiService>,
) : Authenticator {

    private val refreshMutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        val path = response.request.url.encodedPath
        if (path.endsWith("/auth/login") ||
            path.endsWith("/auth/refresh") ||
            path.endsWith("/auth/logout")
        ) {
            Log.d(TAG, "skip refresh on auth endpoint: $path")
            return null
        }

        if (responseCount(response) >= 2) {
            Log.d(TAG, "ya reintentamos antes; abortar")
            return null
        }

        val originalAuthHeader = response.request.header("Authorization")
        val originalToken = originalAuthHeader?.removePrefix("Bearer ")?.trim()

        return runBlocking {
            refreshMutex.withLock {
                val currentToken = tokenStore.accessTokenBlocking()

                // Si otro hilo ya refrescó mientras esperábamos el lock, basta con
                // reintentar la request con el token nuevo.
                if (currentToken != null && currentToken != originalToken) {
                    Log.d(TAG, "token ya refrescado por otro hilo; reintentar")
                    return@withLock buildRequestWith(response.request, currentToken)
                }

                val refreshTok = tokenStore.refreshTokenBlocking()
                if (refreshTok.isNullOrBlank()) {
                    Log.d(TAG, "no hay refresh token; usuario debe re-loguear")
                    return@withLock null
                }

                val newAuth = try {
                    val r = apiServiceProvider.get().refresh(RefreshRequest(refreshTok))
                    if (r.isSuccessful) r.body()?.data else null.also {
                        Log.d(TAG, "refresh devolvió HTTP ${r.code()}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "refresh excepción: ${e.message}")
                    null
                } ?: return@withLock null

                tokenStore.saveTokens(newAuth.accessToken, newAuth.refreshToken)
                Log.d(TAG, "refresh OK; reintentando request original")
                buildRequestWith(response.request, newAuth.accessToken)
            }
        }
    }

    private fun buildRequestWith(original: Request, token: String): Request =
        original.newBuilder().header("Authorization", "Bearer $token").build()

    private fun responseCount(response: Response): Int {
        var current: Response? = response.priorResponse
        var count = 1
        while (current != null) {
            count++
            current = current.priorResponse
        }
        return count
    }

    companion object { private const val TAG = "TokenAuth" }
}
