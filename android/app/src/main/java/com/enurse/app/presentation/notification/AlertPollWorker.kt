package com.enurse.app.presentation.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*
import com.enurse.app.data.remote.api.AlertApi
import com.enurse.app.data.remote.dto.response.AlertResponse
import com.google.gson.Gson
import dagger.hilt.android.EntryPointAccessors
import java.util.concurrent.TimeUnit

/**
 * Periodic background worker that polls GET /alerts?filter=active
 * and fires local phone alarms for new unresolved alerts.
 */
class AlertPollWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "AlertPollWorker"

        fun enqueuePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<AlertPollWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "alert_poll",
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }

        class ResolverReceiver : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val alertId = intent.getStringExtra("alertId") ?: return
                val action = intent.getStringExtra("action") ?: return

                if (action == "resolve") {
                    val entryPoint = EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        AlertApiEntryPoint::class.java
                    )
                    val alertApi = entryPoint.alertApi()

                    Thread {
                        try {
                            alertApi.resolve(alertId)
                            Log.d(TAG, "Alert $alertId resolved via broadcast")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to resolve alert $alertId", e)
                        }
                    }.start()
                }
            }
        }
    }

    override suspend fun doWork(): Result {
        try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                AlertApiEntryPoint::class.java
            )
            val alertApi = entryPoint.alertApi()
            val notificationManager = AlertNotificationManager(applicationContext)

            val alerts = alertApi.getAlerts("active")
            if (alerts.isNotEmpty()) {
                Log.d(TAG, "Found ${alerts.size} active alerts")

                for (alert in alerts) {
                    notificationManager.showAlarm(
                        alert.id, alert.type, alert.message, canResolve = true
                    )
                }
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Alert poll failed", e)
            return Result.retry()
        }
    }
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface AlertApiEntryPoint {
    fun alertApi(): AlertApi
}
