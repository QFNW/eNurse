package com.enurse.app.presentation.navigation

import com.enurse.app.data.local.datastore.TokenDataStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TokenDataStoreEntryPoint {
    fun tokenDataStore(): TokenDataStore
}
