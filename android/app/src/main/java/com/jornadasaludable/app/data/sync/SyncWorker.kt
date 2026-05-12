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

// Worker periódico y on-demand. Delega en OfflineFichajeRepository.syncPendingNow().
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
