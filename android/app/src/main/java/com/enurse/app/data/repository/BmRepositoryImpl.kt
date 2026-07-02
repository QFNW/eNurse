package com.enurse.app.data.repository

import com.enurse.app.data.local.db.dao.BmRecordDao
import com.enurse.app.data.local.db.dao.PendingSyncDao
import com.enurse.app.data.local.db.entity.BmRecordEntity
import com.enurse.app.data.local.db.entity.PendingSyncEntity
import com.enurse.app.data.remote.api.BmApi
import com.enurse.app.data.remote.dto.response.BmCalendarResponse
import com.enurse.app.data.remote.dto.response.BmRecordResponse
import com.enurse.app.data.remote.dto.response.BmSettingsResponse
import com.enurse.app.domain.repository.BmRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class BmRepositoryImpl @Inject constructor(
    private val bmApi: BmApi,
    private val bmRecordDao: BmRecordDao,
    private val pendingSyncDao: PendingSyncDao
) : BmRepository {

    override suspend fun record(userId: String, recordedAt: String, notes: String?): Result<BmRecordResponse> =
        runCatching {
            val body = mutableMapOf("recordedAt" to recordedAt)
            notes?.let { body["notes"] = it }
            val res = bmApi.createRecord(body)
            // Cache locally
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            bmRecordDao.insert(BmRecordEntity(
                id = res.id, userId = userId,
                recordedAt = sdf.parse(res.recordedAt)?.time ?: System.currentTimeMillis(),
                notes = res.notes, synced = true
            ))
            res
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
            jsonData = Gson().toJson(mapOf("recordedAt" to recordedAt, "notes" to notes))
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
                when (item.type) {
                    "bm_record" -> bmApi.createRecord(data)
                }
                pendingSyncDao.delete(item)
            } catch (_: Exception) { /* retry later */ }
        }
    }
}
