package com.enurse.app.presentation.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.enurse.app.MainActivity

class AlertNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "enurse_alerts"
        const val CHANNEL_NAME = "易护提醒"
        const val ALERT_NOTIFICATION_ID = 1001
    }

    fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "大便和用药提醒通知"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 300, 500, 300, 500)
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), null)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun showAlarm(alertId: String, alertType: String, message: String, canResolve: Boolean = true) {
        val manager = context.getSystemService(NotificationManager::class.java)

        // Full-screen intent (opens MainActivity)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Resolve action (swipe to dismiss)
        val resolveIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.enurse.app.RESOLVE_ALERT"
            putExtra("alertId", alertId)
        }
        val resolvePending = PendingIntent.getBroadcast(
            context, alertId.hashCode(), resolveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ 易护提醒")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(openPending, true)
            .setAutoCancel(true)
            .setOngoing(false)
            .setVibrate(longArrayOf(0, 500, 300, 500, 300, 500))
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))

        if (canResolve) {
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "滑动关闭",
                resolvePending
            )
        }

        manager.notify(alertId.hashCode() and 0x7FFFFFFF, builder.build())
    }

    fun cancelAlarm(alertId: String) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancel(alertId.hashCode() and 0x7FFFFFFF)
    }

    fun cancelAll() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancelAll()
    }
}
