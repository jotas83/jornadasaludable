package com.jornadasaludable.app.data.api

import com.google.gson.JsonObject
import com.jornadasaludable.app.data.api.dto.AlertaDto
import com.jornadasaludable.app.data.api.dto.AlertasGenerarResponse
import com.jornadasaludable.app.data.api.dto.AlertasIndexResponse
import com.jornadasaludable.app.data.api.dto.BurnoutResponse
import com.jornadasaludable.app.data.api.dto.CategoriasResponse
import com.jornadasaludable.app.data.api.dto.DerechoBuscarResponse
import com.jornadasaludable.app.data.api.dto.DerechoDto
import com.jornadasaludable.app.data.api.dto.DerechosContenidosResponse
import com.jornadasaludable.app.data.api.dto.DocumentoCreateRequest
import com.jornadasaludable.app.data.api.dto.DocumentoCreateResponse
import com.jornadasaludable.app.data.api.dto.DocumentosIndexResponse
import com.jornadasaludable.app.data.api.dto.EmpresaDto
import com.jornadasaludable.app.data.api.dto.FichajeCreateRequest
import com.jornadasaludable.app.data.api.dto.FichajeCreateResponse
import com.jornadasaludable.app.data.api.dto.FichajesIndexResponse
import com.jornadasaludable.app.data.api.dto.JornadasIndexResponse
import com.jornadasaludable.app.data.api.dto.PerfilDto
import com.jornadasaludable.app.data.api.dto.ResumenDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap
import retrofit2.http.Streaming
import okhttp3.ResponseBody

/**
 * Interfaz Retrofit con los 30 endpoints de la API.
 *
 * Convención: el access-token JWT lo añade un OkHttp interceptor — NO se
 * pasa por @Header en cada método para evitar fugas y reducir boilerplate.
 *
 * Los endpoints públicos (login, refresh, health) no requieren token; el
 * interceptor reconoce sus paths y omite el header Authorization.
 *
 * Tipos de respuesta: donde hay DTO definido (auth) se usa el tipo concreto.
 * Donde aún no hay (mayoría), se devuelve JsonObject para parsearse en el
 * repositorio cuando se implemente la pantalla.
 */
interface ApiService {

    // -------------------------------------------------------------------------
    //  Health (público)
    // -------------------------------------------------------------------------

    @GET("health")
    suspend fun health(): Response<JsonObject>

    // -------------------------------------------------------------------------
    //  Auth
    // -------------------------------------------------------------------------

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<ApiEnvelope<AuthResponse>>

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): Response<ApiEnvelope<AuthResponse>>

    @GET("auth/me")
    suspend fun me(): Response<JsonObject>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

    // -------------------------------------------------------------------------
    //  Jornadas
    // -------------------------------------------------------------------------

    @GET("jornadas")
    suspend fun jornadasIndex(@QueryMap params: Map<String, String> = emptyMap()): Response<ApiEnvelope<JornadasIndexResponse>>

    @GET("jornadas/resumen")
    suspend fun jornadasResumen(): Response<ApiEnvelope<ResumenDto>>

    @GET("jornadas/{uuid}")
    suspend fun jornadasShow(@Path("uuid") uuid: String): Response<JsonObject>

    // -------------------------------------------------------------------------
    //  Fichajes
    // -------------------------------------------------------------------------

    @GET("fichajes")
    suspend fun fichajesIndex(@QueryMap params: Map<String, String> = emptyMap()): Response<ApiEnvelope<FichajesIndexResponse>>

    @POST("fichajes")
    suspend fun fichajeCreate(@Body body: FichajeCreateRequest): Response<ApiEnvelope<FichajeCreateResponse>>

    @POST("fichajes/sync")
    suspend fun fichajesSync(@Body body: Map<String, @JvmSuppressWildcards Any?>): Response<JsonObject>

    // -------------------------------------------------------------------------
    //  Pausas
    // -------------------------------------------------------------------------

    @GET("pausas")
    suspend fun pausasIndex(@QueryMap params: Map<String, String> = emptyMap()): Response<JsonObject>

    @POST("pausas")
    suspend fun pausaCreate(@Body body: Map<String, @JvmSuppressWildcards Any?>): Response<JsonObject>

    // -------------------------------------------------------------------------
    //  Horas extra
    // -------------------------------------------------------------------------

    @GET("horas-extra")
    suspend fun horasExtraIndex(@QueryMap params: Map<String, String> = emptyMap()): Response<JsonObject>

    @POST("horas-extra")
    suspend fun horasExtraCreate(@Body body: Map<String, @JvmSuppressWildcards Any?>): Response<JsonObject>

    // -------------------------------------------------------------------------
    //  Alertas
    // -------------------------------------------------------------------------

    @GET("alertas")
    suspend fun alertasIndex(@QueryMap params: Map<String, String> = emptyMap()): Response<ApiEnvelope<AlertasIndexResponse>>

    @GET("alertas/tipos")
    suspend fun alertasTipos(): Response<JsonObject>

    @POST("alertas/generar")
    suspend fun alertasGenerar(): Response<ApiEnvelope<AlertasGenerarResponse>>

    @PATCH("alertas/{uuid}/leida")
    suspend fun alertasMarcarLeida(@Path("uuid") uuid: String): Response<ApiEnvelope<AlertaDto>>

    // -------------------------------------------------------------------------
    //  Derechos (orden: rutas estáticas declaradas antes en backend; aquí solo
    //  importan las firmas).
    // -------------------------------------------------------------------------

    @GET("derechos/categorias")
    suspend fun derechosCategorias(): Response<ApiEnvelope<CategoriasResponse>>

    @GET("derechos/categorias/{codigo}/contenidos")
    suspend fun derechosContenidos(@Path("codigo") codigo: String): Response<ApiEnvelope<DerechosContenidosResponse>>

    @GET("derechos/buscar")
    suspend fun derechosBuscar(@Query("q") query: String): Response<ApiEnvelope<DerechoBuscarResponse>>

    @GET("derechos/{codigo}")
    suspend fun derechosShow(@Path("codigo") codigo: String): Response<ApiEnvelope<DerechoDto>>

    // -------------------------------------------------------------------------
    //  Documentos
    // -------------------------------------------------------------------------

    @GET("documentos")
    suspend fun documentosIndex(@QueryMap params: Map<String, String> = emptyMap()): Response<ApiEnvelope<DocumentosIndexResponse>>

    @POST("documentos/generar")
    suspend fun documentosGenerar(@Body body: DocumentoCreateRequest): Response<ApiEnvelope<DocumentoCreateResponse>>

    /** Stream binario del PDF — usar Response<ResponseBody> para escribir a fichero. */
    @Streaming
    @GET("documentos/{uuid}/descargar")
    suspend fun documentosDescargar(@Path("uuid") uuid: String): Response<ResponseBody>

    // -------------------------------------------------------------------------
    //  Burnout
    // -------------------------------------------------------------------------

    @GET("burnout")
    suspend fun burnoutIndex(@QueryMap params: Map<String, String> = emptyMap()): Response<ApiEnvelope<BurnoutResponse>>

    // -------------------------------------------------------------------------
    //  Usuario (perfil propio + empresa actual)
    // -------------------------------------------------------------------------

    @GET("usuarios/perfil")
    suspend fun usuariosPerfil(): Response<ApiEnvelope<PerfilDto>>

    @PUT("usuarios/perfil")
    suspend fun usuariosUpdatePerfil(@Body body: Map<String, @JvmSuppressWildcards Any?>): Response<ApiEnvelope<PerfilDto>>

    @GET("usuarios/empresa")
    suspend fun usuariosEmpresa(): Response<ApiEnvelope<EmpresaDto>>
}
