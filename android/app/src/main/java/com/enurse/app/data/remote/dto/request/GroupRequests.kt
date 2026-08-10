package com.enurse.app.data.remote.dto.request

/**
 * 护理端请求 DTO。
 * 注意：Retrofit @Body 不能用 Map<String, Any>——Kotlin Any 编译为 Java
 * 通配符（Map<String, ?>），Gson 反射序列化时抛
 * "parameter type must not include a type variable or wildcard"。
 * 必须用具体类。
 */

data class CreateMedicationForPatientRequest(
    val name: String,
    val dosage: String? = null,
    val scheduleType: String? = null,
    val intervalHours: Double? = null,
    val fixedTimes: List<String>? = null
)

data class UpdateMemberStrategiesRequest(
    val intervalDays: Int? = null,
    val reminderTime: String? = null,
    val isEnabled: Boolean? = null,
    val scheduleId: String? = null,
    val scheduleType: String? = null,
    val intervalHours: Double? = null,
    val fixedTimes: List<String>? = null,
    val startTime: String? = null
)
