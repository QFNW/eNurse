package com.enurse.app

import android.app.Application
import com.enurse.app.presentation.notification.AlertNotificationManager
import com.enurse.app.presentation.notification.AlertPollWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class eNurseApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Create notification channel for alert alarms
        AlertNotificationManager(this).createChannel()

        // Start periodic alert polling
        AlertPollWorker.enqueuePeriodic(this)
    }
}
