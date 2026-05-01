package com.jornadasaludable.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Base de datos Room.
 *
 * v1 → UserEntity (cache del trabajador autenticado)
 * v2 → +FichajeEntity (cola offline para SyncWorker)
 *
 * `fallbackToDestructiveMigration` configurado en DatabaseModule: el upgrade
 * v1→v2 borra y recrea. En MVP es aceptable; el usuario re-loguea y los
 * fichajes pendientes (si los hubiera) se perderían — improbable porque la
 * tabla nueva no existía antes del bump.
 */
@Database(
    entities = [UserEntity::class, FichajeEntity::class],
    version  = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao():    UserDao
    abstract fun fichajeDao(): FichajeDao

    companion object {
        const val NAME = "jornadasaludable.db"
    }
}
