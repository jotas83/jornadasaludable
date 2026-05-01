package com.jornadasaludable.app.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jornadasaludable.app.data.repository.OfflineFichajeRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException

/**
 * Worker periódico (15 min con NETWORK_CONNECTED) y on-demand. Delega la
 * lógica de sync a `OfflineFichajeRepository.syncPendingNow()` para que la
 * misma implementación sirva tanto al worker como al botón "Sincronizar
 * ahora" del Fragment.
 *
 *   - Sin pendientes → success() inmediato (no consume datos).
 *   - IOException    → retry() (WorkManager backoff exponencial).
 *   - Otro error     → failure() (no insistas).
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val offlineFichajeRepository: OfflineFichajeRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork() start")
        return offlineFichajeRepository.syncPendingNow().fold(
            onSuccess = { count ->
                Log.d(TAG, "synced $count fichajes → success()")
                Result.success()
            },
            onFailure = { e ->
                if (e is IOException) {
                    Log.d(TAG, "IOException → retry(): ${e.message}")
                    Result.retry()
                } else {
                    Log.w(TAG, "fail → failure(): ${e.message}")
                    Result.failure()
                }
            },
        )
    }

    companion object { private const val TAG = "SyncWorker" }
}
