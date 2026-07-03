package com.enurse.app.presentation.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmScheduler(private val context: Context) {

    companion object {
        const val RETRY_DELAY_MED = 15 * 60 * 1000L  // 15 minutes
        const val RETRY_DELAY_BM = 2 * 60 * 60 * 1000L // 2 hours
        const val TIMEOUT_MS = 100 * 1000L             // 100 seconds
    }

    /**
     * Schedule an immediate alarm notification.
     */
    fun scheduleImmediate(alertId: String, alertType: String, message: String) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alertId", alertId)
            putExtra("alertType", alertType)
            putExtra("message", message)
            putExtra("canResolve", true)
        }
        val pending = PendingIntent.getBroadcast(
            context, alertId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), pending
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), pending
            )
        }
    }

    /**
     * Schedule a retry alarm after the timeout period.
     * medication: 15min, BM: 2h
     */
    fun scheduleRetry(alertId: String, alertType: String, message: String) {
        val delay = if (alertType == "MEDICATION_REMINDER") RETRY_DELAY_MED else RETRY_DELAY_BM

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alertId", alertId)
            putExtra("alertType", alertType)
            putExtra("message", message)
            putExtra("canResolve", true)
        }
        val pending = PendingIntent.getBroadcast(
            context, "retry_${alertId}".hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delay, pending
        )
    }

    /**
     * Schedule a timeout check: if alert still unresolved after 100s, trigger retry.
     */
    fun scheduleTimeout(alertId: String, alertType: String, message: String) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alertId", alertId)
            putExtra("alertType", alertType)
            putExtra("message", "$message\n\n(未解除，稍后再次提醒)")
            putExtra("canResolve", true)
        }
        val pending = PendingIntent.getBroadcast(
            context, "timeout_${alertId}".hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + TIMEOUT_MS, pending
        )
    }

    fun cancelAlarms(alertId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (prefix in listOf("", "retry_", "timeout_")) {
            val intent = Intent(context, AlarmReceiver::class.java)
            val pending = PendingIntent.getBroadcast(
                context, "${prefix}${alertId}".hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pending)
        }
    }
}
