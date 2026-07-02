package com.enurse.app.data.remote.dto.response

import com.google.gson.annotations.SerializedName

data class MedicationResponse(
    val id: String,
    val name: String,
    val dosage: String?,
    val unit: String?,
    val notes: String?,
    val isActive: Boolean,
    val schedules: List<ScheduleResponse>?
)

data class ScheduleResponse(
    val id: String,
    val medicationId: String,
    val scheduleType: String,
    val intervalHours: Int?,
    val fixedTimes: List<String>?,
    val isEnabled: Boolean,
    val isLocked: Boolean
)

data class MedicationRecordResponse(
    val id: String,
    val medicationId: String,
    val userId: String,
    val takenAt: String,
    val medication: MedicationBrief?
)

data class MedicationBrief(
    val id: String,
    val name: String
)

data class AlertResponse(
    val id: String,
    val userId: String,
    val type: String,
    val message: String,
    val isResolved: Boolean,
    val createdAt: String
)

data class GroupResponse(
    val id: String,
    val name: String,
    val inviteCode: String,
    val createdBy: String,
    val _count: GroupCount?,
    val members: List<GroupMemberResponse>?
)

data class GroupCount(
    val members: Int
)

data class GroupMemberResponse(
    val id: String,
    val groupId: String,
    val userId: String,
    val roleInGroup: String,
    val user: UserBrief?,
    val alertCount: Int?,
    val group: GroupBrief?
)

data class GroupBrief(
    val id: String?,
    val name: String?
)
