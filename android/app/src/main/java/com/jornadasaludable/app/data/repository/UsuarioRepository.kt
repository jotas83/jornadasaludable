package com.jornadasaludable.app.data.repository

import com.google.gson.Gson
import com.jornadasaludable.app.data.api.ApiService
import com.jornadasaludable.app.data.api.dto.EmpresaDto
import com.jornadasaludable.app.data.api.dto.PerfilDto
import com.jornadasaludable.app.data.api.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsuarioRepository @Inject constructor(
    private val api: ApiService,
    private val gson: Gson,
) {
    suspend fun perfil():  Result<PerfilDto>  = safeApiCall(gson) { api.usuariosPerfil() }
    suspend fun empresa(): Result<EmpresaDto> = safeApiCall(gson) { api.usuariosEmpresa() }
}
