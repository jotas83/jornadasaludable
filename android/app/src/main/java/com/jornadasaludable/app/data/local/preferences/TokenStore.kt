package com.jornadasaludable.app.data.local.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore by preferencesDataStore(name = "auth_prefs")

/**
 * Persistencia de tokens JWT vía DataStore Preferences. Storage cifrado por
 * defecto del sistema; el fichero queda excluido de auto-backup
 * (data_extraction_rules.xml + backup_rules.xml).
 *
 * Lecturas síncronas (`accessTokenBlocking`) solo desde threads no-main:
 * el AuthInterceptor de OkHttp corre en thread de red.
 */
@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val ACCESS  = stringPreferencesKey("access_token")
        val REFRESH = stringPreferencesKey("refresh_token")
    }

    val accessToken:  Flow<String?> = context.authDataStore.data.map { it[Keys.ACCESS] }
    val refreshToken: Flow<String?> = context.authDataStore.data.map { it[Keys.REFRESH] }

    suspend fun saveTokens(access: String, refresh: String) {
        context.authDataStore.edit { prefs ->
            prefs[Keys.ACCESS]  = access
            prefs[Keys.REFRESH] = refresh
        }
    }

    suspend fun clear() {
        context.authDataStore.edit { it.clear() }
    }

    /** Lectura bloqueante para uso desde OkHttp Interceptor. NO llamar en main thread. */
    fun accessTokenBlocking(): String? = runBlocking { accessToken.first() }
}
