package com.enurse.app.presentation.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Reschedules alert polling after device reboot.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            AlertPollWorker.enqueuePeriodic(context)
        }
    }
}
