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

// Resuelve 401 refrescando el access token con el refresh token.
// Provider<ApiService> rompe el ciclo OkHttp → Authenticator → ApiService.
// Mutex evita refrescos simultáneos cuando varios 401 caen a la vez.
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

                // Otro hilo pudo refrescar mientras esperábamos el lock.
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
