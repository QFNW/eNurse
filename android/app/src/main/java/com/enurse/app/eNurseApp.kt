package com.enurse.app

import android.app.Application
import com.enurse.app.presentation.polling.AlertPollService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class eNurseApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Start background alert polling (30s interval, rings on new alerts)
        AlertPollService.start(this)
    }
}
