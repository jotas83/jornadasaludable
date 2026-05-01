package com.jornadasaludable.app.data.repository

import com.google.gson.Gson
import com.jornadasaludable.app.data.api.ApiService
import com.jornadasaludable.app.data.api.dto.DocumentoCreateRequest
import com.jornadasaludable.app.data.api.dto.DocumentoCreateResponse
import com.jornadasaludable.app.data.api.dto.DocumentoDto
import com.jornadasaludable.app.data.api.safeApiCall
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentoRepository @Inject constructor(
    private val api: ApiService,
    private val gson: Gson,
) {
    suspend fun list(): Result<List<DocumentoDto>> =
        safeApiCall(gson) {
            api.documentosIndex(mapOf("limit" to "50"))
        }.map { resp -> resp.items }

    suspend fun generarMensual(mes: String): Result<DocumentoCreateResponse> =
        safeApiCall(gson) {
            api.documentosGenerar(
                DocumentoCreateRequest(tipo = "REGISTRO_JORNADA_MENSUAL", mes = mes)
            )
        }

    /**
     * Descarga el PDF a `target` (típicamente context.cacheDir/pdf_{uuid}.pdf).
     * Usa el endpoint @Streaming para no cargar todo el cuerpo en memoria.
     */
    suspend fun download(uuid: String, target: File): Result<File> {
        return try {
            val resp = api.documentosDescargar(uuid)
            if (!resp.isSuccessful) {
                return Result.failure(Exception("HTTP ${resp.code()} al descargar."))
            }
            val body = resp.body() ?: return Result.failure(IOException("Respuesta sin cuerpo."))
            body.byteStream().use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            Result.success(target)
        } catch (e: IOException) {
            Result.failure(IOException("Sin conexión: ${e.message ?: "no se pudo descargar."}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
