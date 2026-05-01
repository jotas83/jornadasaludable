package com.jornadasaludable.app

import androidx.hilt.work.HiltWorkerFactory
import android.app.Application
import androidx.work.Configuration
import com.jornadasaludable.app.data.notifications.AlertaNotificationService
import com.jornadasaludable.app.data.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application root. Implementa Configuration.Provider para que WorkManager
 * use el HiltWorkerFactory generado y pueda inyectar dependencias en los
 * @HiltWorker (sin esto SyncWorker NO se instancia y el sync offline no corre).
 *
 * El initializer por defecto de WorkManager está deshabilitado en
 * AndroidManifest.xml (tools:node="remove") para evitar doble init.
 *
 * onCreate dispara también una sync inmediata one-time: si hay pendientes Y
 * red, corre ya; si no hay red, queda parked hasta que vuelva (auto-sync on
 * reconnect). Más el periodic 15-min como red de seguridad.
 */
@HiltAndroidApp
class JornadaSaludableApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var syncScheduler: SyncScheduler
    @Inject lateinit var alertaNotificationService: AlertaNotificationService

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        alertaNotificationService.ensureChannel()
        syncScheduler.schedulePeriodicSync()
        syncScheduler.scheduleImmediateSync()
    }
}
