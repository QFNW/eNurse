package com.enurse.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medication_records")
data class MedicationRecordEntity(
    @PrimaryKey val id: String,
    val medicationId: String,
    val medicationName: String,
    val userId: String,
    val takenAt: Long,
    val synced: Boolean = false
)
