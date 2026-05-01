package com.jornadasaludable.app.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Punto único para programar sincronizaciones del SyncWorker.
 *
 *   schedulePeriodicSync()  — desde Application.onCreate(); cada 15 min con
 *                             constraint NETWORK_CONNECTED. Idempotente
 *                             (ExistingPeriodicWorkPolicy.KEEP).
 *
 *   scheduleImmediateSync() — desde el repo cuando se persiste un fichaje
 *                             offline; one-time con la misma constraint, así
 *                             que WorkManager espera a que vuelva la red y
 *                             dispara el SyncWorker en cuanto haya conexión.
 *                             Esto es el "auto-sync on reconnect".
 */
@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val PERIODIC_NAME  = "sync-fichajes-periodic"
        private const val IMMEDIATE_NAME = "sync-fichajes-immediate"
        private const val PERIODIC_INTERVAL_MIN = 15L
    }

    private val networkConstraint = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun schedulePeriodicSync() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(
            PERIODIC_INTERVAL_MIN, TimeUnit.MINUTES,
        ).setConstraints(networkConstraint).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun scheduleImmediateSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(networkConstraint)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}
