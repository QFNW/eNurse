package com.enurse.app.data.remote.dto.response

import com.google.gson.annotations.SerializedName

data class BmRecordResponse(
    val id: String,
    val userId: String,
    val recordedAt: String,
    val notes: String?,
    val createdAt: String
)

data class BmCalendarResponse(
    val year: Int,
    val month: Int,
    val days: List<CalendarDay>
)

data class CalendarDay(
    val date: String,
    val hasRecord: Boolean,
    val records: List<BmRecordResponse>
)

data class BmSettingsResponse(
    val id: String,
    val userId: String,
    val intervalDays: Int,
    val reminderTime: String,
    val isEnabled: Boolean,
    val isLocked: Boolean
)
