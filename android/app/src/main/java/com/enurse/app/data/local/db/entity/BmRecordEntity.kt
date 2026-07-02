package com.enurse.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bm_records")
data class BmRecordEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val recordedAt: Long,  // epoch millis
    val notes: String?,
    val synced: Boolean = false
)
