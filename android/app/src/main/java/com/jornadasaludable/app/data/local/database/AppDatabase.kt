package com.jornadasaludable.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase

// v1: UserEntity. v2: + FichajeEntity (cola offline).
// El upgrade es destructivo (DatabaseModule lo configura así).
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
