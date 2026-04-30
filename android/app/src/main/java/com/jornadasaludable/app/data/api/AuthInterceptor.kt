package com.jornadasaludable.app.data.api

import com.jornadasaludable.app.data.local.preferences.TokenStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adjunta el header `Authorization: Bearer <access_token>` a toda petición
 * a endpoints autenticados. Se salta los endpoints públicos (health, login,
 * refresh) para que NO viaje un token caducado en peticiones que no lo
 * necesitan.
 *
 * Lee el token de DataStore vía `accessTokenBlocking` — el interceptor corre
 * en thread de red (no en main), así que el bloqueo es seguro.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
) : Interceptor {

    private val publicSuffixes = listOf(
        "/health",
        "/auth/login",
        "/auth/refresh",
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        if (publicSuffixes.any { path.endsWith(it) }) {
            return chain.proceed(request)
        }

        val token = tokenStore.accessTokenBlocking()
        val authed = if (token.isNullOrBlank()) {
            request
        } else {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(authed)
    }
}
