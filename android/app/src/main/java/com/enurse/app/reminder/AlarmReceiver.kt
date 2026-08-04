package com.enurse.app.reminder

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.enurse.app.MainActivity
import com.enurse.app.presentation.reminder.ReminderActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 本地闹钟触发点（Manifest receiver，进程被杀也会被系统唤醒）。
 *
 * 流程：查本地 Room「已打卡则跳过」→ 未跳过则发通知（铃声/振动由系统通知管理，受 DND 约束）
 * + 启动全屏 ReminderActivity（亮屏）→ 无论跳不跳都重排下一次。
 */
class AlarmReceiver : BroadcastReceiver() {
    private val tag = "AlarmReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderScheduler.ACTION_ALARM_FIRE) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handle(context.applicationContext, intent)
            } catch (e: Exception) {
                Log.w(tag, "handle failed: ${e.message}")
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun handle(context: Context, intent: Intent) {
        val ep = ReminderScheduler.entryPoint(context)
        val token = ep.tokenDataStore().accessToken.first()
        if (token == null) {
            // 登出兜底：内存态可能已丢失（进程被杀后登出），触发时自毁
            Log.i(tag, "token null — cancelling alarm")
            ReminderScheduler.cancelAll(context)
            return
        }
        val userId = ep.tokenDataStore().userId.first() ?: return
        val type = intent.getStringExtra(ReminderScheduler.EXTRA_TYPE) ?: return
        val now = System.currentTimeMillis()
        val fireAt = intent.getLongExtra(ReminderScheduler.EXTRA_FIRE_AT, now)

        // 已打卡跳过（对齐服务端 alert-engine 语义）
        val alreadyTaken = when (type) {
            "MEDICATION" -> {
                val medId = intent.getStringExtra(ReminderScheduler.EXTRA_MEDICATION_ID) ?: ""
                ep.medicationDao()
                    .getByMedicationIdAndRange(medId, fireAt - 3_600_000L, now)
                    .isNotEmpty()
            }
            "BM" -> {
                val days = intent.getIntExtra(ReminderScheduler.EXTRA_INTERVAL_DAYS, 1).coerceAtLeast(1)
                ep.bmRecordDao()
                    .getByUserAndRange(userId, now - days * 86_400_000L, now)
                    .isNotEmpty()
            }
            else -> false
        }

        if (alreadyTaken) {
            Log.i(tag, "已打卡，跳过本次提醒 (type=$type)")
        } else {
            ReminderScheduler.noteFired(type)
            showReminder(context, intent)
        }

        // 无论是否跳过都重排下一次
        ReminderScheduler.scheduleNextFromExtras(context, intent)
    }

    /** 发通知（声音/振动随系统 DND 管理）+ 启动全屏亮屏页。 */
    private fun showReminder(context: Context, intent: Intent) {
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE) ?: "提醒"
        val body = intent.getStringExtra(ReminderScheduler.EXTRA_BODY) ?: ""

        // 通知：铃声 + 振动（受 DND 约束）
        val contentPi = PendingIntent.getActivity(
            context, intent.getStringExtra(ReminderScheduler.EXTRA_SCHEDULE_ID)?.hashCode()?.and(0x7FFFFFFF) ?: 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .setVibrate(longArrayOf(0, 500, 300, 500, 300, 500))
            .setContentIntent(contentPi)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify((ReminderScheduler.EXTRA_SCHEDULE_ID + intent.getLongExtra(ReminderScheduler.EXTRA_FIRE_AT, 0)).hashCode().and(0x7FFFFFFF), notification)

        // 全屏亮屏页
        try {
            val act = Intent(context, ReminderActivity::class.java).apply {
                putExtras(intent)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(act)
        } catch (e: Exception) {
            // OEM 后台启动限制兜底：降级为全屏通知（Android 14+ 无权限时退化为 heads-up）
            Log.w(tag, "startActivity failed, falling back to full-screen notification: ${e.message}")
            val fullScreenPi = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val fs = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(fullScreenPi, true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                .setVibrate(longArrayOf(0, 500, 300, 500, 300, 500))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .notify((ReminderScheduler.EXTRA_SCHEDULE_ID + System.currentTimeMillis()).hashCode().and(0x7FFFFFFF), fs)
        }
    }

}
