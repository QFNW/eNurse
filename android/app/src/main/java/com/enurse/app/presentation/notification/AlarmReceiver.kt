package com.enurse.app.presentation.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alertId = intent.getStringExtra("alertId") ?: return
        val alertType = intent.getStringExtra("alertType") ?: "BM_REMINDER"
        val message = intent.getStringExtra("message") ?: "提醒"
        val canResolve = intent.getBooleanExtra("canResolve", true)

        val notificationManager = AlertNotificationManager(context)
        notificationManager.showAlarm(alertId, alertType, message, canResolve)

        if (intent.action == "com.enurse.app.RESOLVE_ALERT") {
            // Resolve action — handled by AlertPollWorker via API call
            val resolveIntent = Intent(context, AlertPollWorker.Companion.ResolverReceiver::class.java).apply {
                putExtra("alertId", alertId)
                putExtra("action", "resolve")
            }
            context.sendBroadcast(resolveIntent)

            notificationManager.cancelAlarm(alertId)
        }
    }
}
