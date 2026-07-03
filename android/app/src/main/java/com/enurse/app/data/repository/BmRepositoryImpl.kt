package com.enurse.app.data.repository

import com.enurse.app.data.local.db.dao.BmRecordDao
import com.enurse.app.data.local.db.dao.MedicationDao
import com.enurse.app.data.local.db.dao.PendingSyncDao
import com.enurse.app.data.local.db.entity.BmRecordEntity
import com.enurse.app.data.local.db.entity.MedicationRecordEntity
import com.enurse.app.data.local.db.entity.PendingSyncEntity
import com.enurse.app.data.remote.api.BmApi
import com.enurse.app.data.remote.api.MedicationApi
import com.enurse.app.data.remote.dto.response.BmCalendarResponse
import com.enurse.app.data.remote.dto.response.BmRecordResponse
import com.enurse.app.data.remote.dto.response.BmSettingsResponse
import android.util.Log
import com.enurse.app.domain.repository.BmRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class BmRepositoryImpl @Inject constructor(
    private val bmApi: BmApi,
    private val medicationApi: MedicationApi,
    private val bmRecordDao: BmRecordDao,
    private val medicationDao: MedicationDao,
    private val pendingSyncDao: PendingSyncDao
) : BmRepository {

    override suspend fun record(userId: String, recordedAt: String, notes: String?): Result<BmRecordResponse> {
        val body = mutableMapOf("recordedAt" to recordedAt)
        notes?.let { body["notes"] = it }

        // API call first — if it fails, don't cache locally
        val apiResult = runCatching { bmApi.createRecord(body) }
        if (apiResult.isFailure) return apiResult

        val res = apiResult.getOrThrow()
        // Cache locally (best-effort, failure is non-fatal since server has the record)
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            bmRecordDao.insert(BmRecordEntity(
                id = res.id, userId = userId,
                recordedAt = sdf.parse(res.recordedAt)?.time ?: System.currentTimeMillis(),
                notes = res.notes, synced = true
            ))
        } catch (e: Exception) {
            Log.e("BmRepository", "Failed to cache BM record locally", e)
        }
        return Result.success(res)
    }

    override suspend fun recordOffline(userId: String, recordedAt: String, notes: String?) {
        val id = "local_${UUID.randomUUID()}"
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        bmRecordDao.insert(BmRecordEntity(
            id = id, userId = userId,
            recordedAt = sdf.parse(recordedAt)?.time ?: System.currentTimeMillis(),
            notes = notes, synced = false
        ))
        pendingSyncDao.insert(PendingSyncEntity(
            type = "bm_record",
            jsonData = Gson().toJson(mapOf(
                "recordedAt" to recordedAt,
                "notes" to (notes ?: "")
            )),
            localRecordId = id
        ))
    }

    override suspend fun getCalendar(year: Int, month: Int): Result<BmCalendarResponse> =
        runCatching { bmApi.getCalendar(year, month) }

    override suspend fun getSettings(): Result<BmSettingsResponse> =
        runCatching { bmApi.getSettings() }

    override suspend fun updateSettings(intervalDays: Int?, reminderTime: String?, isEnabled: Boolean?): Result<BmSettingsResponse> =
        runCatching {
            val body = mutableMapOf<String, Any>()
            intervalDays?.let { body["intervalDays"] = it }
            reminderTime?.let { body["reminderTime"] = it }
            isEnabled?.let { body["isEnabled"] = it }
            bmApi.updateSettings(body)
        }

    override suspend fun deleteRecord(id: String) {
        bmApi.deleteRecord(id)
        bmRecordDao.delete(BmRecordEntity(id = id, userId = "", recordedAt = 0))
    }

    override fun getLocalRecords(userId: String): Flow<List<BmRecordEntity>> =
        bmRecordDao.getByUser(userId)

    override suspend fun syncPending() {
        val unsynced = pendingSyncDao.getAll()
        for (item in unsynced) {
            try {
                val data = Gson().fromJson(item.jsonData, Map::class.java) as Map<String, String>
                val synced = when (item.type) {
                    "bm_record" -> {
                        val serverRecord = bmApi.createRecord(data)
                        // Insert new record first; only delete old after successful insert
                        bmRecordDao.insert(BmRecordEntity(
                            id = serverRecord.id,
                            userId = serverRecord.userId,
                            recordedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                                .parse(serverRecord.recordedAt)?.time ?: System.currentTimeMillis(),
                            notes = serverRecord.notes,
                            synced = true
                        ))
                        if (item.localRecordId.isNotEmpty()) {
                            bmRecordDao.delete(BmRecordEntity(
                                id = item.localRecordId, userId = "", recordedAt = 0
                            ))
                        }
                        true
                    }
                    "medication_record" -> {
                        val serverRecord = medicationApi.createRecord(data)
                        medicationDao.insert(MedicationRecordEntity(
                            id = serverRecord.id,
                            medicationId = serverRecord.medicationId,
                            medicationName = serverRecord.medication?.name ?: "",
                            userId = serverRecord.userId,
                            takenAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                                .parse(serverRecord.takenAt)?.time ?: System.currentTimeMillis(),
                            synced = true
                        ))
                        if (item.localRecordId.isNotEmpty()) {
                            medicationDao.delete(MedicationRecordEntity(
                                id = item.localRecordId, medicationId = "",
                                medicationName = "", userId = "", takenAt = 0
                            ))
                        }
                        true
                    }
                    else -> false
                }
                if (synced) pendingSyncDao.delete(item)
            } catch (e: Exception) { Log.e("BmRepository", "syncPending failed for item ${item.id}", e) }
        }
    }
}
