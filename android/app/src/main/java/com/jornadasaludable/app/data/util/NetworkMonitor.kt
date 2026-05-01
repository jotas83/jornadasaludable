package com.jornadasaludable.app.data.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper sobre ConnectivityManager para una pregunta concreta:
 * "¿hay red disponible AHORA mismo?".
 *
 * Útil cuando WorkManager tarda en detectar el cambio de constraint
 * NETWORK_CONNECTED (típico en emuladores con WiFi alternada): si esta
 * función devuelve true, podemos hacer la petición síncrona desde una
 * coroutine en lugar de esperar al worker.
 *
 * Solo comprueba `NET_CAPABILITY_INTERNET`; no exige `VALIDATED` porque en
 * algunos emuladores (sin DNS válido) Android no marca la capability
 * VALIDATED aunque haya conectividad real.
 */
@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun isOnline(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
