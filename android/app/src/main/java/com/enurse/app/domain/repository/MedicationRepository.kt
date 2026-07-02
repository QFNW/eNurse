package com.enurse.app.domain.repository

import com.enurse.app.data.local.db.entity.MedicationRecordEntity
import com.enurse.app.data.remote.dto.response.MedicationResponse
import com.enurse.app.data.remote.dto.response.MedicationRecordResponse
import kotlinx.coroutines.flow.Flow

interface MedicationRepository {
    suspend fun getMedications(): Result<List<MedicationResponse>>
    suspend fun createMedication(name: String, dosage: String? = null): Result<MedicationResponse>
    suspend fun deleteMedication(id: String)
    suspend fun record(medicationId: String, takenAt: String): Result<MedicationRecordResponse>
    suspend fun recordOffline(medicationId: String, medicationName: String, takenAt: String)
    fun getLocalRecords(userId: String): Flow<List<MedicationRecordEntity>>
}
