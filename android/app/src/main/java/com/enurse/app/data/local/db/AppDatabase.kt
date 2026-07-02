package com.enurse.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.enurse.app.data.local.db.dao.BmRecordDao
import com.enurse.app.data.local.db.dao.MedicationDao
import com.enurse.app.data.local.db.dao.PendingSyncDao
import com.enurse.app.data.local.db.entity.BmRecordEntity
import com.enurse.app.data.local.db.entity.MedicationRecordEntity
import com.enurse.app.data.local.db.entity.PendingSyncEntity

@Database(
    entities = [BmRecordEntity::class, MedicationRecordEntity::class, PendingSyncEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bmRecordDao(): BmRecordDao
    abstract fun medicationDao(): MedicationDao
    abstract fun pendingSyncDao(): PendingSyncDao
}
