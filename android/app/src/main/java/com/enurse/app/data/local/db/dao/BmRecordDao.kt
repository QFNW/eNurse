package com.enurse.app.data.local.db.dao

import androidx.room.*
import com.enurse.app.data.local.db.entity.BmRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BmRecordDao {
    @Query("SELECT * FROM bm_records WHERE userId = :userId ORDER BY recordedAt DESC")
    fun getByUser(userId: String): Flow<List<BmRecordEntity>>

    @Query("SELECT * FROM bm_records WHERE userId = :userId AND recordedAt BETWEEN :start AND :end ORDER BY recordedAt ASC")
    suspend fun getByUserAndRange(userId: String, start: Long, end: Long): List<BmRecordEntity>

    @Query("SELECT * FROM bm_records WHERE synced = 0")
    suspend fun getUnsynced(): List<BmRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: BmRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<BmRecordEntity>)

    @Query("UPDATE bm_records SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Delete
    suspend fun delete(record: BmRecordEntity)
}
