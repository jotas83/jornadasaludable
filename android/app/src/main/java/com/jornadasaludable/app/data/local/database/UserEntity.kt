package com.jornadasaludable.app.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jornadasaludable.app.data.api.UserDto

// Cache del trabajador autenticado. Una sola fila viva; el logout vacía la tabla.
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: Long,
    val uuid: String,
    val nif: String,
    val nombre: String,
    val apellidos: String,
    val email: String?,
    val idioma: String?,
    val cachedAt: Long = System.currentTimeMillis(),
) {
    fun toDto() = UserDto(
        id = id, uuid = uuid, nif = nif,
        nombre = nombre, apellidos = apellidos,
        email = email, idioma = idioma,
    )

    companion object {
        fun fromDto(dto: UserDto) = UserEntity(
            id = dto.id, uuid = dto.uuid, nif = dto.nif,
            nombre = dto.nombre, apellidos = dto.apellidos,
            email = dto.email, idioma = dto.idioma,
        )
    }
}
