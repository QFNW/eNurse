package com.enurse.app.data.local.db.dao

import androidx.room.*
import com.enurse.app.data.local.db.entity.MedicationRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medication_records WHERE userId = :userId ORDER BY takenAt DESC")
    fun getByUser(userId: String): Flow<List<MedicationRecordEntity>>

    /** 某药品在 [start, end] 时间窗内的打卡记录（本地闹钟「已打卡则跳过」判断用） */
    @Query("SELECT * FROM medication_records WHERE medicationId = :medicationId AND takenAt BETWEEN :start AND :end")
    suspend fun getByMedicationIdAndRange(medicationId: String, start: Long, end: Long): List<MedicationRecordEntity>

    @Query("SELECT * FROM medication_records WHERE synced = 0")
    suspend fun getUnsynced(): List<MedicationRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: MedicationRecordEntity)

    @Query("UPDATE medication_records SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Delete
    suspend fun delete(record: MedicationRecordEntity)
}
