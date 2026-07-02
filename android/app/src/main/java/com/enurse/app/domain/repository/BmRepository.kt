package com.enurse.app.domain.repository

import com.enurse.app.data.local.db.entity.BmRecordEntity
import com.enurse.app.data.remote.dto.response.BmCalendarResponse
import com.enurse.app.data.remote.dto.response.BmRecordResponse
import com.enurse.app.data.remote.dto.response.BmSettingsResponse
import kotlinx.coroutines.flow.Flow

interface BmRepository {
    suspend fun record(userId: String, recordedAt: String, notes: String? = null): Result<BmRecordResponse>
    suspend fun recordOffline(userId: String, recordedAt: String, notes: String? = null)
    suspend fun getCalendar(year: Int, month: Int): Result<BmCalendarResponse>
    suspend fun getSettings(): Result<BmSettingsResponse>
    suspend fun updateSettings(intervalDays: Int? = null, reminderTime: String? = null, isEnabled: Boolean? = null): Result<BmSettingsResponse>
    suspend fun deleteRecord(id: String)
    fun getLocalRecords(userId: String): Flow<List<BmRecordEntity>>
    suspend fun syncPending()
}
