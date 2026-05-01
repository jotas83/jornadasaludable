package com.jornadasaludable.app.data.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.jornadasaludable.app.data.api.ApiService
import com.jornadasaludable.app.data.api.dto.FichajeCreateRequest
import com.jornadasaludable.app.data.api.dto.FichajeCreateResponse
import com.jornadasaludable.app.data.local.database.FichajeDao
import com.jornadasaludable.app.data.local.database.FichajeEntity
import com.jornadasaludable.app.data.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositorio offline-first para crear y sincronizar fichajes.
 *
 * `crearFichaje`:
 *   1. Intenta POST /fichajes inmediato.
 *   2. Si éxito              → Synced(response).
 *   3. Si HTTP 4xx            → Failure(message) — error semántico, NO se
 *                              persiste local (reintentar no arregla un fichaje
 *                              rechazado por sin contrato/licencia).
 *   4. Si IOException         → persiste en Room PENDING + scheduleImmediateSync.
 *
 * `syncPendingNow`: ejecuta el cuerpo de sync directamente en una coroutine
 * (sin pasar por WorkManager). Lo usan tanto el SyncWorker (delegación) como
 * el botón "Sincronizar ahora" cuando NetworkMonitor confirma que hay red,
 * para evitar la latencia del constraint NETWORK_CONNECTED del worker.
 */
@Singleton
class OfflineFichajeRepository @Inject constructor(
    private val api: ApiService,
    private val gson: Gson,
    private val fichajeDao: FichajeDao,
    private val syncScheduler: SyncScheduler,
) {
    suspend fun crearFichaje(req: FichajeCreateRequest): Result<FichajeOutcome> {
        require(req.uuid != null) { "uuid client-side obligatorio para idempotencia/offline" }

        return try {
            val resp = api.fichajeCreate(req)
            if (resp.isSuccessful) {
                val data = resp.body()?.data
                    ?: return Result.failure(IOException("Respuesta sin datos."))
                Result.success(FichajeOutcome.Synced(data))
            } else {
                val msg = parseErrorMessage(resp.errorBody()?.string())
                    ?: defaultHttpMessage(resp.code())
                Result.failure(Exception(msg))
            }
        } catch (e: IOException) {
            fichajeDao.upsert(FichajeEntity.fromRequest(req))
            syncScheduler.scheduleImmediateSync()
            Result.success(FichajeOutcome.QueuedOffline(req.uuid, req.tipo))
        }
    }

    /**
     * Sincroniza pendientes ahora mismo. Devuelve el número de fichajes
     * confirmados por el backend. Si falla por IO, devuelve Failure para que
     * el caller decida (reintentar luego, mostrar error).
     */
    suspend fun syncPendingNow(): Result<Int> {
        val pending = fichajeDao.getByStatus(FichajeEntity.STATUS_PENDING)
        Log.d(TAG, "syncPendingNow: ${pending.size} pendientes")
        if (pending.isEmpty()) return Result.success(0)

        val payload: Map<String, Any?> = mapOf("fichajes" to pending.map(::toApiItem))
        return try {
            val resp = api.fichajesSync(payload)
            Log.d(TAG, "POST /fichajes/sync → ${resp.code()}")
            if (!resp.isSuccessful) {
                return Result.failure(Exception("HTTP ${resp.code()}"))
            }
            val body = resp.body()
            val results = body?.getAsJsonObject("data")?.getAsJsonArray("results")

            if (results == null) {
                // Sin "results" parseables → asumimos éxito global (HTTP 200).
                fichajeDao.deleteByUuids(pending.map { it.uuid })
                return Result.success(pending.size)
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
            Log.d(TAG, "syncPendingNow OK: ${toDelete.size} sincronizados")
            Result.success(toDelete.size)
        } catch (e: IOException) {
            Log.d(TAG, "syncPendingNow IOException: ${e.message}")
            Result.failure(e)
        }
    }

    fun pendingCountFlow(): Flow<Int> =
        fichajeDao.countByStatus(FichajeEntity.STATUS_PENDING)

    suspend fun pendingCountOnce(): Int =
        fichajeDao.countByStatusOnce(FichajeEntity.STATUS_PENDING)

    // ---------- helpers ----------

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

    private fun parseErrorMessage(body: String?): String? {
        if (body.isNullOrBlank()) return null
        return runCatching {
            val obj = gson.fromJson(body, JsonObject::class.java)
            obj?.getAsJsonObject("error")?.get("message")?.asString
        }.getOrNull()
    }

    private fun defaultHttpMessage(code: Int): String = when (code) {
        401 -> "Sesión caducada. Vuelve a iniciar sesión."
        403 -> "No tienes permiso o falta contrato/licencia vigente."
        422 -> "Datos no válidos."
        in 500..599 -> "Error del servidor ($code)."
        else -> "Error HTTP $code"
    }

    companion object { private const val TAG = "OfflineFichaje" }
}

sealed interface FichajeOutcome {
    data class Synced(val response: FichajeCreateResponse) : FichajeOutcome
    data class QueuedOffline(val uuid: String, val tipo: String) : FichajeOutcome
}
