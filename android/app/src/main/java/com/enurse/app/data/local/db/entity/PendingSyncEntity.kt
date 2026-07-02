package com.enurse.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_sync")
data class PendingSyncEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,      // "bm_record" | "medication_record"
    val jsonData: String,  // serialized request body
    val createdAt: Long = System.currentTimeMillis()
)
