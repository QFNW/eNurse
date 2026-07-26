package com.enurse.app.presentation.polling

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.enurse.app.MainActivity
import com.enurse.app.data.remote.api.AlertApi
import com.enurse.app.data.remote.dto.response.AlertResponse
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AlertPollService {
    private const val TAG = "AlertPollService"
    const val CHANNEL_ID = "enurse_medication_alert"
    private const val POLL_INTERVAL = 30_000L

    private val _alerts = MutableStateFlow<List<AlertResponse>>(emptyList())
    val alerts: StateFlow<List<AlertResponse>> = _alerts

    private var job: Job? = null
    private var lastIds = emptySet<String>()
    private var api: AlertApi? = null

    fun start(app: Application) {
        if (job?.isActive == true) return
        val ep = EntryPointAccessors.fromApplication(app, AlertApiEntryPoint::class.java)
        api = ep.alertApi()
        createNotificationChannel(app)

        job = CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
            fetchAlerts(app)
            while (isActive) {
                delay(POLL_INTERVAL)
                fetchAlerts(app)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        lastIds = emptySet()
    }

    /** Remove alert from local state immediately (call after server resolve succeeds) */
    fun removeFromLocal(alertId: String) {
        _alerts.value = _alerts.value.filter { it.id != alertId }
    }

    private suspend fun fetchAlerts(context: Context) {
        try {
            val current = api?.getAlerts("active") ?: return
            _alerts.value = current
            val newIds = current.map { it.id }.toSet()
            val fresh = current.filter { it.id !in lastIds }
            lastIds = newIds

            if (fresh.isNotEmpty()) {
                Log.i(TAG, "New alerts: ${fresh.size}")
                for (alert in fresh) showNotification(context, alert)
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchAlerts failed: ${e.message}")
        }
    }

    private fun showNotification(context: Context, alert: AlertResponse) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val reqCode = alert.id.hashCode() and 0x7FFFFFFF
            val pendingIntent = PendingIntent.getActivity(
                context, reqCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val bigText = if (alert.message.length > 80) {
                NotificationCompat.BigTextStyle().bigText(alert.message)
            } else NotificationCompat.BigTextStyle()

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("⏰ 用药提醒")
                .setContentText(alert.message)
                .setStyle(bigText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                .setVibrate(longArrayOf(0, 500, 300, 500, 300, 500))
                .setContentIntent(pendingIntent)
                .setFullScreenIntent(pendingIntent, true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(reqCode, notification)
        } catch (e: Exception) {
            Log.e(TAG, "showNotification: ${e.message}")
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(CHANNEL_ID, "用药提醒", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "用药和大便提醒通知"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 300, 500, 300, 500)
                setSound(alarmSound, audioAttrs)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    @EntryPoint @InstallIn(SingletonComponent::class)
    interface AlertApiEntryPoint {
        fun alertApi(): AlertApi
    }
}
