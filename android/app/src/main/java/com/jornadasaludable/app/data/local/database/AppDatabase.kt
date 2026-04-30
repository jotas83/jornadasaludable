package com.jornadasaludable.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Base de datos Room. Por ahora solo hostea UserEntity para cachear el
 * trabajador autenticado; se ampliará con FichajeEntity/JornadaEntity/etc.
 * a medida que se construya el módulo offline.
 */
@Database(
    entities = [UserEntity::class],
    version  = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        const val NAME = "jornadasaludable.db"
    }
}
