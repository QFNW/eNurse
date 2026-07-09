package com.enurse.app.presentation.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.enurse.app.data.remote.dto.response.ScheduleResponse
import java.util.*

object AlarmScheduler {
    private const val TAG = "AlarmScheduler"

    fun scheduleMedicationAlarms(context: Context, schedules: List<Pair<String, ScheduleResponse>>) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val now = System.currentTimeMillis()

            for ((medName, schedule) in schedules) {
                val times = when (schedule.scheduleType) {
                    "FIXED_TIME" -> schedule.fixedTimes?.mapNotNull { parseTime(it) } ?: emptyList()
                    "INTERVAL" -> {
                        val hours = schedule.intervalHours ?: 8.0
                        if (hours <= 0) continue
                        val intervalMs = (hours * 3600_000).toLong()
                        val count = ((48 * 3600_000) / intervalMs).toInt().coerceIn(1, 48)
                        (0 until count).map { now + (it + 1) * intervalMs }
                    }
                    else -> emptyList()
                }

                for (timeMs in times) {
                    if (timeMs <= now + 30_000) continue

                    val intent = Intent(context, AlarmReceiver::class.java).apply {
                        putExtra("medicationName", medName)
                        putExtra("scheduleId", schedule.id)
                    }
                    val requestCode = (schedule.id + timeMs.toString()).hashCode()
                    val flag = PendingIntent.FLAG_UPDATE_CURRENT or
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

                    try {
                        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flag)
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMs, pendingIntent)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to schedule alarm: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "scheduleMedicationAlarms failed: ${e.message}")
        }
    }

    fun cancelAll(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, AlarmReceiver::class.java)
            val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_NO_CREATE or flag)
            pendingIntent?.let { alarmManager.cancel(it) }
        } catch (e: Exception) {
            Log.e(TAG, "cancelAll failed: ${e.message}")
        }
    }

    private fun parseTime(time: String): Long? {
        return try {
            val parts = time.split(":")
            if (parts.size != 2) return null
            val hour = parts[0].toIntOrNull() ?: return null
            val minute = parts[1].toIntOrNull() ?: return null
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            if (cal.timeInMillis <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1)
            cal.timeInMillis
        } catch (e: Exception) { null }
    }
}
