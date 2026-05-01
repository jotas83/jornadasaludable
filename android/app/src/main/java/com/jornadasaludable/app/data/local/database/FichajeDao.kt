package com.jornadasaludable.app.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FichajeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FichajeEntity)

    @Query("SELECT * FROM fichajes_pending WHERE syncStatus = :status ORDER BY createdAt ASC")
    suspend fun getByStatus(status: String = FichajeEntity.STATUS_PENDING): List<FichajeEntity>

    @Query("DELETE FROM fichajes_pending WHERE uuid IN (:uuids)")
    suspend fun deleteByUuids(uuids: List<String>)

    @Query("UPDATE fichajes_pending SET syncStatus = :status, lastError = :error WHERE uuid = :uuid")
    suspend fun markStatus(uuid: String, status: String, error: String?)

    @Query("SELECT COUNT(*) FROM fichajes_pending WHERE syncStatus = :status")
    fun countByStatus(status: String = FichajeEntity.STATUS_PENDING): Flow<Int>

    @Query("SELECT COUNT(*) FROM fichajes_pending WHERE syncStatus = :status")
    suspend fun countByStatusOnce(status: String = FichajeEntity.STATUS_PENDING): Int
}
