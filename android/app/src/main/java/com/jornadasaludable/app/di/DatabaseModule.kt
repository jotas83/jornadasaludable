package com.jornadasaludable.app.di

import android.content.Context
import androidx.room.Room
import com.jornadasaludable.app.data.local.database.AppDatabase
import com.jornadasaludable.app.data.local.database.FichajeDao
import com.jornadasaludable.app.data.local.database.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    fun provideFichajeDao(db: AppDatabase): FichajeDao = db.fichajeDao()
}
