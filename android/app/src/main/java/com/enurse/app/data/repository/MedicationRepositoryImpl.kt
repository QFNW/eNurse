package com.enurse.app.data.repository

import com.enurse.app.data.local.datastore.TokenDataStore
import com.enurse.app.data.local.db.dao.MedicationDao
import com.enurse.app.data.local.db.dao.PendingSyncDao
import com.enurse.app.data.local.db.entity.MedicationRecordEntity
import com.enurse.app.data.local.db.entity.PendingSyncEntity
import com.enurse.app.data.remote.api.MedicationApi
import com.enurse.app.data.remote.dto.response.MedicationResponse
import com.enurse.app.data.remote.dto.response.MedicationRecordResponse
import com.enurse.app.domain.repository.MedicationRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class MedicationRepositoryImpl @Inject constructor(
    private val medicationApi: MedicationApi,
    private val medicationDao: MedicationDao,
    private val pendingSyncDao: PendingSyncDao,
    private val tokenDataStore: TokenDataStore
) : MedicationRepository {

    override suspend fun getMedications(): Result<List<MedicationResponse>> =
        runCatching { medicationApi.getAll() }

    override suspend fun createMedication(name: String, dosage: String?): Result<MedicationResponse> =
        runCatching {
            val body = mutableMapOf("name" to name)
            dosage?.let { body["dosage"] = it }
            medicationApi.create(body)
        }

    override suspend fun deleteMedication(id: String) {
        medicationApi.delete(id)
    }

    override suspend fun createSchedule(medicationId: String, scheduleType: String, times: List<String>, intervalHours: Double?): Result<Any> =
        runCatching {
            val body = mutableMapOf<String, Any>("scheduleType" to scheduleType)
            if (scheduleType == "FIXED_TIME") body["fixedTimes"] = times
            if (intervalHours != null) body["intervalHours"] = intervalHours
            medicationApi.createSchedule(medicationId, body)
        }

    override suspend fun getRecords(): Result<List<MedicationRecordResponse>> =
        runCatching { medicationApi.getRecords() }

    override suspend fun deleteSchedule(scheduleId: String) {
        medicationApi.deleteSchedule(scheduleId)
    }

    override suspend fun record(userId: String, medicationId: String, takenAt: String, notes: String?): Result<MedicationRecordResponse> =
        runCatching {
            val body = mutableMapOf("medicationId" to medicationId, "takenAt" to takenAt)
            if (notes != null) body["notes"] = notes
            val res = medicationApi.createRecord(body)
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            medicationDao.insert(MedicationRecordEntity(
                id = res.id, medicationId = medicationId,
                medicationName = res.medication?.name ?: "",
                userId = userId, takenAt = sdf.parse(res.takenAt)?.time ?: System.currentTimeMillis(),
                synced = true
            ))
            res
        }

    override suspend fun recordOffline(userId: String, medicationId: String, medicationName: String, takenAt: String) {
        val id = "local_${UUID.randomUUID()}"
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        medicationDao.insert(MedicationRecordEntity(
            id = id, medicationId = medicationId, medicationName = medicationName,
            userId = userId, takenAt = sdf.parse(takenAt)?.time ?: System.currentTimeMillis(), synced = false
        ))
        pendingSyncDao.insert(PendingSyncEntity(
            type = "medication_record",
            jsonData = Gson().toJson(mapOf("medicationId" to medicationId, "takenAt" to takenAt)),
            localRecordId = id
        ))
    }

    override fun getLocalRecords(userId: String): Flow<List<MedicationRecordEntity>> =
        medicationDao.getByUser(userId)
}
