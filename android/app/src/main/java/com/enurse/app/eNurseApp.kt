package com.enurse.app

import android.app.Application
import com.enurse.app.data.local.datastore.TokenDataStore
import com.enurse.app.presentation.navigation.TokenDataStoreEntryPoint
import com.enurse.app.presentation.polling.AlertPollService
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class eNurseApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // EntryPoint (not field injection): injecting into the Application
        // itself would create an Application → TokenDataStore → Context cycle.
        val tokenDataStore = EntryPointAccessors.fromApplication(
            this, TokenDataStoreEntryPoint::class.java
        ).tokenDataStore()

        // Poll alerts only while logged in: token present → start,
        // cleared (logout / refresh failure) → stop. start() is idempotent.
        appScope.launch {
            tokenDataStore.accessToken.collect { token ->
                if (token != null) {
                    AlertPollService.start(this@eNurseApp)
                } else {
                    AlertPollService.stop()
                }
            }
        }
    }
}
