package com.jornadasaludable.app.data.repository

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
 * Repositorio offline-first para crear fichajes:
 *
 *   1. Intenta POST /fichajes inmediato.
 *   2. Si succeeds  → devuelve Synced(response).
 *   3. Si HTTP 4xx  → devuelve Failure(message) — error semántico, NO se
 *                     persiste local (sería un fichaje rechazado que reintentar
 *                     no arregla — empresa inactiva, sin contrato vigente, etc.).
 *   4. Si IOException (sin red, timeout, etc.) → persiste en Room con status
 *      PENDING y agenda un OneTimeWorkRequest del SyncWorker. Devuelve
 *      QueuedOffline. Cuando vuelva la red WorkManager lo despachará.
 *
 * El UUID del cliente garantiza idempotencia tanto en la API como en Room
 * (PrimaryKey). Si por carrera dos workers envían el mismo, el backend
 * devuelve `idempotent: true` y el segundo intento acaba siendo no-op.
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
                // Rechazo semántico — no encolamos para reintentar.
                val msg = parseErrorMessage(resp.errorBody()?.string())
                    ?: defaultHttpMessage(resp.code())
                Result.failure(Exception(msg))
            }
        } catch (e: IOException) {
            // Sin red: persistimos local + agendamos sync.
            fichajeDao.upsert(FichajeEntity.fromRequest(req))
            syncScheduler.scheduleImmediateSync()
            Result.success(FichajeOutcome.QueuedOffline(req.uuid, req.tipo))
        }
    }

    fun pendingCountFlow(): Flow<Int> =
        fichajeDao.countByStatus(FichajeEntity.STATUS_PENDING)

    suspend fun pendingCountOnce(): Int =
        fichajeDao.countByStatusOnce(FichajeEntity.STATUS_PENDING)

    // ---------- helpers ----------

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
}

sealed interface FichajeOutcome {
    data class Synced(val response: FichajeCreateResponse) : FichajeOutcome
    data class QueuedOffline(val uuid: String, val tipo: String) : FichajeOutcome
}
