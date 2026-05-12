package com.jornadasaludable.app.data.api

import com.jornadasaludable.app.data.local.preferences.TokenStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

// Añade Authorization: Bearer en las peticiones autenticadas.
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
