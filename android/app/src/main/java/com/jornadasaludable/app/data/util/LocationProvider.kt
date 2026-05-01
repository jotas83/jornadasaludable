package com.jornadasaludable.app.data.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Wrapper sobre `android.location.LocationManager` — sin dependencia de
 * Google Play Services. Útil para builds AOSP/sin-GMS y para reducir el
 * tamaño del APK.
 *
 * - `lastKnownLocation()` es no-bloqueante (cacheado). Puede devolver null
 *   si nunca se ha obtenido fix o si los permisos no están concedidos.
 * - `requestSingleUpdate()` pide UNA actualización fresca con timeout.
 *   Útil al pulsar ENTRADA/SALIDA para obtener la posición real.
 *
 * El llamador es responsable de pedir los permisos en runtime.
 */
@Singleton
class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val lm: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun hasPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun isGpsEnabled():     Boolean = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
    fun isNetworkEnabled(): Boolean = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

    /**
     * Última posición cacheada. Recorre los providers activos (GPS primero,
     * luego network, luego passive) y devuelve la más reciente.
     */
    fun lastKnownLocation(): Location? {
        if (!hasPermission()) return null
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        ).filter { lm.allProviders.contains(it) && lm.isProviderEnabled(it) }

        var best: Location? = null
        for (p in providers) {
            try {
                val loc = lm.getLastKnownLocation(p) ?: continue
                if (best == null || loc.time > best.time) best = loc
            } catch (_: SecurityException) {
                return null
            }
        }
        return best
    }

    /**
     * Pide un único update fresco. Devuelve null si timeout o sin permiso.
     * Usar antes de un fichaje para registrar coordenadas reales.
     */
    suspend fun requestSingleUpdate(timeoutMs: Long = 5_000L): Location? {
        if (!hasPermission()) return null
        val provider = when {
            isGpsEnabled()     -> LocationManager.GPS_PROVIDER
            isNetworkEnabled() -> LocationManager.NETWORK_PROVIDER
            else               -> return lastKnownLocation()
        }

        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<Location?> { cont ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        try { lm.removeUpdates(this) } catch (_: SecurityException) {}
                        if (cont.isActive) cont.resume(location)
                    }
                    @Deprecated("Required by older API")
                    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        lm.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
                    } else {
                        @Suppress("DEPRECATION")
                        lm.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
                    }
                } catch (_: SecurityException) {
                    if (cont.isActive) cont.resume(null)
                    return@suspendCancellableCoroutine
                }
                cont.invokeOnCancellation {
                    try { lm.removeUpdates(listener) } catch (_: SecurityException) {}
                }
            }
        } ?: lastKnownLocation()
    }
}
