package com.enurse.app.presentation.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*
import com.enurse.app.data.local.datastore.TokenDataStore
import com.enurse.app.data.remote.api.AlertApi
import com.google.gson.Gson
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Periodic background worker that polls GET /alerts?filter=active
 * and fires local phone alarms for new unresolved alerts.
 * Uses DataStore to track already-fired alert IDs (dedup).
 */
class AlertPollWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "AlertPollWorker"
        private const val LAST_SEEN_KEY = "last_seen_alert_ids"

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
                    ExistingPeriodicWorkPolicy.UPDATE,
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
                    val tokenDataStore = entryPoint.tokenDataStore()

                    Thread {
                        try {
                            alertApi.resolve(alertId)
                            Log.d(TAG, "Alert $alertId resolved via broadcast")
                            // Also remove from local tracking so it doesn't re-fire
                            // (cleaned up on next poll cycle)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to resolve alert $alertId — will retry next poll", e)
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
            val tokenDataStore = entryPoint.tokenDataStore()
            val notificationManager = AlertNotificationManager(applicationContext)

            // Read previously fired alert IDs from DataStore
            val seenJson = tokenDataStore.getPreference(LAST_SEEN_KEY) ?: ""
            val seenIds = if (seenJson.isNotEmpty())
                Gson().fromJson(seenJson, Array<String>::class.java).toMutableSet()
            else mutableSetOf<String>()

            val alerts = alertApi.getAlerts("active")
            if (alerts.isNotEmpty()) {
                Log.d(TAG, "Found ${alerts.size} active alerts, ${seenIds.size} already seen")

                var newCount = 0
                for (alert in alerts) {
                    if (alert.id !in seenIds) {
                        notificationManager.showAlarm(alert.id, alert.type, alert.message)
                        seenIds.add(alert.id)
                        newCount++
                    }
                }

                // Persist updated seen set (trim to last 200 to avoid unbounded growth)
                val trimmed = seenIds.takeLast(200).toTypedArray()
                tokenDataStore.setPreference(LAST_SEEN_KEY, Gson().toJson(trimmed))

                if (newCount > 0) {
                    Log.d(TAG, "Fired $newCount new alert notifications")
                }
            }

            // Clean up seen IDs that are no longer active
            val activeIds = alerts.map { it.id }.toSet()
            seenIds.retainAll(activeIds)
            val trimmed = seenIds.takeLast(200).toTypedArray()
            tokenDataStore.setPreference(LAST_SEEN_KEY, Gson().toJson(trimmed))

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
    fun tokenDataStore(): TokenDataStore
}
