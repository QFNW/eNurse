package com.enurse.app.domain.model

data class User(
    val id: String,
    val email: String,
    val role: String,
    val nickname: String?
)

data class BmRecord(
    val id: String,
    val recordedAt: String,
    val notes: String?
)

data class Medication(
    val id: String,
    val name: String,
    val dosage: String?,
    val unit: String?,
    val schedules: List<Schedule>
)

data class Schedule(
    val id: String,
    val scheduleType: String,
    val intervalHours: Int?,
    val fixedTimes: List<String>?,
    val isEnabled: Boolean,
    val isLocked: Boolean
)

data class Alert(
    val id: String,
    val type: String,
    val message: String,
    val isResolved: Boolean,
    val createdAt: String
)

data class Group(
    val id: String,
    val name: String,
    val inviteCode: String,
    val memberCount: Int
)

data class GroupMember(
    val id: String,
    val userId: String,
    val email: String?,
    val nickname: String?,
    val roleInGroup: String,
    val alertCount: Int
)
