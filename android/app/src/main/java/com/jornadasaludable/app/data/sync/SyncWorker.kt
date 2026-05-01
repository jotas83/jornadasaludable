package com.jornadasaludable.app.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jornadasaludable.app.data.api.ApiService
import com.jornadasaludable.app.data.local.database.FichajeDao
import com.jornadasaludable.app.data.local.database.FichajeEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException

/**
 * Worker periódico (15 min con NetworkType.CONNECTED) y on-demand que
 * envía los fichajes en cola a POST /fichajes/sync y limpia los confirmados.
 *
 * - Sin pendientes → success() inmediato (no consume datos).
 * - Network IOException → retry() (WorkManager backoff exponencial).
 * - HTTP 4xx por cuerpo → marca como FAILED para no entrar en bucle infinito.
 * - HTTP 5xx → retry() (problema servidor, reintentamos).
 *
 * El backend devuelve un array `results` con `status: created | duplicate
 * | error` por uuid. Borramos los `created` y `duplicate` (ambos significan
 * "ya está en BD"). Los `error` los marcamos FAILED con el mensaje.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val fichajeDao: FichajeDao,
    private val api: ApiService,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val pending = fichajeDao.getByStatus(FichajeEntity.STATUS_PENDING)
        if (pending.isEmpty()) return Result.success()

        val payload: Map<String, Any?> = mapOf(
            "fichajes" to pending.map(::toApiItem)
        )

        return try {
            val resp = api.fichajesSync(payload)
            if (!resp.isSuccessful) {
                // 4xx semántico (validación) → no insistas. 5xx → reintenta.
                return if (resp.code() in 500..599) Result.retry() else Result.failure()
            }
            val body = resp.body()
            val results = body?.getAsJsonArray("data")
                ?.takeIf { it.size() > 0 }
                ?.firstOrNull()                      // por si el backend envuelve en data { results: [...] }
                ?.asJsonObject?.getAsJsonArray("results")
                ?: body?.getAsJsonArray("results")
                ?: body?.getAsJsonObject("data")?.getAsJsonArray("results")

            // Estrategia conservadora: si no podemos parsear results, asumimos
            // éxito global (HTTP 200) y borramos todos los pendientes enviados.
            if (results == null) {
                fichajeDao.deleteByUuids(pending.map { it.uuid })
                return Result.success()
            }

            val toDelete = mutableListOf<String>()
            for (i in 0 until results.size()) {
                val obj = results[i].asJsonObject
                val uuid = obj.get("uuid")?.asString ?: continue
                when (obj.get("status")?.asString) {
                    "created", "duplicate" -> toDelete.add(uuid)
                    "error" -> {
                        val msg = obj.get("message")?.asString ?: "error sin mensaje"
                        fichajeDao.markStatus(uuid, FichajeEntity.STATUS_FAILED, msg)
                    }
                }
            }
            if (toDelete.isNotEmpty()) fichajeDao.deleteByUuids(toDelete)
            Result.success()
        } catch (e: IOException) {
            Result.retry()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun toApiItem(e: FichajeEntity): Map<String, Any?> = mapOf(
        "uuid"             to e.uuid,
        "tipo"             to e.tipo,
        "timestamp_evento" to e.timestampEvento,
        "latitud"          to e.latitud,
        "longitud"         to e.longitud,
        "precision_gps_m"  to e.precisionGpsM,
        "dentro_geofence"  to e.dentroGeofence,
        "metodo"           to e.metodo,
        "device_id"        to e.deviceId,
    )
}
