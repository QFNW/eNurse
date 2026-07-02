package com.enurse.app.di

import com.enurse.app.data.repository.*
import com.enurse.app.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindBmRepository(impl: BmRepositoryImpl): BmRepository

    @Binds @Singleton
    abstract fun bindMedicationRepository(impl: MedicationRepositoryImpl): MedicationRepository

    @Binds @Singleton
    abstract fun bindGroupRepository(impl: GroupRepositoryImpl): GroupRepository

    @Binds @Singleton
    abstract fun bindAlertRepository(impl: AlertRepositoryImpl): AlertRepository
}
