package com.enurse.app.data.local.db.dao

import androidx.room.*
import com.enurse.app.data.local.db.entity.MedicationRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medication_records WHERE userId = :userId ORDER BY takenAt DESC")
    fun getByUser(userId: String): Flow<List<MedicationRecordEntity>>

    @Query("SELECT * FROM medication_records WHERE synced = 0")
    suspend fun getUnsynced(): List<MedicationRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: MedicationRecordEntity)

    @Query("UPDATE medication_records SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
}
