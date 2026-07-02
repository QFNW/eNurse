package com.enurse.app.data.local.db.dao

import androidx.room.*
import com.enurse.app.data.local.db.entity.PendingSyncEntity

@Dao
interface PendingSyncDao {
    @Query("SELECT * FROM pending_sync ORDER BY createdAt ASC")
    suspend fun getAll(): List<PendingSyncEntity>

    @Insert
    suspend fun insert(entity: PendingSyncEntity)

    @Delete
    suspend fun delete(entity: PendingSyncEntity)

    @Query("DELETE FROM pending_sync")
    suspend fun clearAll()
}
