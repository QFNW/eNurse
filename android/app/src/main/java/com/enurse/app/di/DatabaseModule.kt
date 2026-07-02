package com.enurse.app.di

import android.content.Context
import androidx.room.Room
import com.enurse.app.data.local.db.AppDatabase
import com.enurse.app.data.local.db.dao.BmRecordDao
import com.enurse.app.data.local.db.dao.MedicationDao
import com.enurse.app.data.local.db.dao.PendingSyncDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "enurse.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideBmRecordDao(db: AppDatabase): BmRecordDao = db.bmRecordDao()
    @Provides fun provideMedicationDao(db: AppDatabase): MedicationDao = db.medicationDao()
    @Provides fun providePendingSyncDao(db: AppDatabase): PendingSyncDao = db.pendingSyncDao()
}
