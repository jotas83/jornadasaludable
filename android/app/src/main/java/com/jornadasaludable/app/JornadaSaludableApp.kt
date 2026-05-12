package com.jornadasaludable.app

import androidx.hilt.work.HiltWorkerFactory
import android.app.Application
import androidx.work.Configuration
import com.jornadasaludable.app.data.notifications.AlertaNotificationService
import com.jornadasaludable.app.data.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

// Configuration.Provider para que WorkManager use el HiltWorkerFactory
// (sin esto SyncWorker no recibe dependencias). El initializer por defecto
// de WorkManager está deshabilitado en el manifest.
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
