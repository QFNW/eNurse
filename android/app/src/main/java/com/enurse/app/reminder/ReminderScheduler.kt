package com.enurse.app.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.enurse.app.data.local.db.dao.BmRecordDao
import com.enurse.app.data.local.db.dao.MedicationDao
import com.enurse.app.data.local.datastore.TokenDataStore
import com.enurse.app.data.remote.dto.response.BmSettingsResponse
import com.enurse.app.data.remote.dto.response.MedicationResponse
import com.enurse.app.domain.repository.BmRepository
import com.enurse.app.domain.repository.MedicationRepository
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * 本地闹钟调度（AlarmManager setExactAndAllowWhileIdle）。
 *
 * 职责：拉服务端 schedules/settings → 算下一次触发 → 注册闹钟；触发后（AlarmReceiver）重排。
 * 语义对齐服务端 alert-engine：打卡后跳过（FIXED_TIME/INTERVAL 1 小时内，BM intervalDays 天内）；
 * INTERVAL 从本地锚点（上次响铃）起算，锚点存 TokenDataStore KV——登出 clear() 自动清除。
 */
object ReminderScheduler {
    private const val TAG = "ReminderScheduler"

    const val ACTION_ALARM_FIRE = "com.enurse.app.action.ALARM_FIRE"
    const val CHANNEL_ID = "enurse_medication_alert" // 复用 AlertPollService 现有渠道

    // Intent extras（AlarmReceiver → ReminderActivity 全链路携带）
    const val EXTRA_TYPE = "type"                    // "MEDICATION" | "BM"
    const val EXTRA_SCHEDULE_ID = "scheduleId"
    const val EXTRA_MEDICATION_ID = "medicationId"
    const val EXTRA_TITLE = "title"
    const val EXTRA_BODY = "body"
    const val EXTRA_FIXED_TIMES = "fixedTimes"       // ArrayList<String>，仅 FIXED_TIME
    const val EXTRA_INTERVAL_HOURS = "intervalHours" // Double，仅 INTERVAL
    const val EXTRA_REMINDER_TIME = "reminderTime"   // 仅 BM
    const val EXTRA_INTERVAL_DAYS = "intervalDays"   // Int，仅 BM
    const val EXTRA_FIRE_AT = "fireAt"               // Long，本次计划触发时刻
    const val EXTRA_SNOOZE = "snooze"                // Boolean，稍后提醒

    private const val ANCHOR_PREFIX = "alarm_anchor_"
    private const val BM_SCHEDULE_ID = "bm_reminder"
    const val SNOOZE_MS = 5 * 60_000L

    /** 已注册的 requestCode（内存态；进程被杀后登出靠触发时 token 检查自毁兜底） */
    private val registeredCodes = ConcurrentHashMap.newKeySet<Int>()

    /** 最近触发时刻（type → firedAt），供 AlertPollService 抑制与服务端重复的双响通知 */
    private val recentFires = ConcurrentHashMap<String, Long>()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ── 对外 API ──

    /** 拉服务端数据 → 全量重排。网络失败时保留旧闹钟。 */
    fun refresh(context: Context) {
        scope.launch {
            try {
                val ep = entryPoint(context)
                val token = ep.tokenDataStore().accessToken.first()
                val userId = ep.tokenDataStore().userId.first()
                // userId 未就绪（登录态写入竞态）→ 跳过，等下次调用（eNurseApp collect / 保存后 refresh）
                if (token == null || userId == null) return@launch

                val meds = ep.medicationRepository().getMedications().getOrNull()
                val bm = ep.bmRepository().getSettings().getOrNull()
                if (meds == null && bm == null) {
                    Log.w(TAG, "refresh: 网络失败，保留旧闹钟")
                    return@launch
                }

                cancelAll(context)

                val now = System.currentTimeMillis()
                meds?.forEach { med -> scheduleMedication(context, ep, med, now) }
                bm?.let { scheduleBm(context, ep, it, now) }
            } catch (e: Exception) {
                Log.w(TAG, "refresh failed: ${e.message}")
            }
        }
    }

    /** 取消全部本地闹钟（登出时调用）。内存态不完整时，残留闹钟由 AlarmReceiver 触发时 token 检查自毁。 */
    fun cancelAll(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java)
        for (rc in registeredCodes) {
            am.cancel(pendingIntentFor(context, rc))
        }
        registeredCodes.clear()
    }

    /** AlarmReceiver 用：根据触发时的快照数据重排下一次（纯本地，不联网）。 */
    fun scheduleNextFromExtras(context: Context, fired: Intent) {
        try {
            val type = fired.getStringExtra(EXTRA_TYPE) ?: return
            val now = System.currentTimeMillis()

            if (fired.getBooleanExtra(EXTRA_SNOOZE, false)) {
                // 稍后提醒：+5 分钟再响，不清除锚点
                reRegister(context, fired, now + SNOOZE_MS)
                return
            }

            when (type) {
                "MEDICATION" -> {
                    val hours = fired.getDoubleExtra(EXTRA_INTERVAL_HOURS, -1.0)
                    if (hours > 0) {
                        // INTERVAL：锚点推进到本次响铃时刻
                        val fireAt = fired.getLongExtra(EXTRA_FIRE_AT, now)
                        scope.launch {
                            try {
                                entryPoint(context).tokenDataStore()
                                    .setPreference(ANCHOR_PREFIX + fired.getStringExtra(EXTRA_SCHEDULE_ID), fireAt.toString())
                            } catch (e: Exception) {
                                Log.w(TAG, "save anchor failed: ${e.message}")
                            }
                        }
                        val next = ReminderTimeCalculator.nextInterval(fireAt, hours, now)
                        reRegister(context, fired, next)
                    } else {
                        // FIXED_TIME：下一个未过时刻（今天/明天）
                        val times = fired.getStringArrayListExtra(EXTRA_FIXED_TIMES) ?: emptyList()
                        val next = ReminderTimeCalculator.nextFixedTime(times, now) ?: return
                        reRegister(context, fired, next)
                    }
                }
                "BM" -> {
                    val t = fired.getStringExtra(EXTRA_REMINDER_TIME) ?: return
                    val next = ReminderTimeCalculator.nextBmReminder(t, now) ?: return
                    reRegister(context, fired, next)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "scheduleNextFromExtras failed: ${e.message}")
        }
    }

    /** 记录一次真实触发（供双响抑制）。 */
    fun noteFired(type: String) {
        recentFires[type] = System.currentTimeMillis()
    }

    /** [windowMs] 内是否已触发过 [type]（AlertPollService 通知前检查）。 */
    fun recentlyFiredWithin(type: String, windowMs: Long): Boolean {
        val t = recentFires[type] ?: return false
        return System.currentTimeMillis() - t < windowMs
    }

    /** 是否具备精确闹钟授权（API 31+ 需 SCHEDULE_EXACT_ALARM；API < 31 恒 true）。 */
    fun canScheduleExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    /** 把服务端 alert.type 映射到抑制类型；非用药/大便提醒返回 null（不抑制，避免误吞护理警报）。 */
    fun mapAlertType(type: String?): String? = when (type) {
        "MEDICATION_REMINDER" -> "MEDICATION"
        "BM_REMINDER" -> "BM"
        else -> null
    }

    // ── 内部：注册 ──

    private suspend fun scheduleMedication(
        context: Context,
        ep: ReminderDataEntryPoint,
        med: MedicationResponse,
        now: Long
    ) {
        if (!med.isActive) return
        med.schedules.orEmpty().forEach { s ->
            if (!s.isEnabled) return@forEach
            val fireAt = when (s.scheduleType) {
                "FIXED_TIME" -> ReminderTimeCalculator.nextFixedTime(s.fixedTimes.orEmpty(), now)
                "INTERVAL" -> {
                    val h = s.intervalHours ?: return@forEach
                    if (h <= 0) return@forEach
                    val anchor = getAnchor(ep, s.id)
                    ReminderTimeCalculator.nextInterval(anchor, h, now)
                }
                else -> null
            } ?: return@forEach

            val bundle = Bundle().apply {
                putString(EXTRA_TYPE, "MEDICATION")
                putString(EXTRA_SCHEDULE_ID, s.id)
                putString(EXTRA_MEDICATION_ID, med.id)
                putString(EXTRA_TITLE, "💊 用药提醒")
                putString(EXTRA_BODY, buildMedBody(med))
                putStringArrayList(EXTRA_FIXED_TIMES, ArrayList(s.fixedTimes.orEmpty()))
                putDouble(EXTRA_INTERVAL_HOURS, s.intervalHours ?: -1.0)
                putLong(EXTRA_FIRE_AT, fireAt)
            }
            register(context, fireAt, bundle)
        }
    }

    private suspend fun scheduleBm(context: Context, ep: ReminderDataEntryPoint, s: BmSettingsResponse, now: Long) {
        if (!s.isEnabled) return
        val fireAt = ReminderTimeCalculator.nextBmReminder(s.reminderTime, now) ?: return
        val bundle = Bundle().apply {
            putString(EXTRA_TYPE, "BM")
            putString(EXTRA_SCHEDULE_ID, BM_SCHEDULE_ID)
            putString(EXTRA_TITLE, "💩 大便提醒")
            putString(EXTRA_BODY, "该记录今天的大便情况了")
            putString(EXTRA_REMINDER_TIME, s.reminderTime)
            putInt(EXTRA_INTERVAL_DAYS, s.intervalDays)
            putLong(EXTRA_FIRE_AT, fireAt)
        }
        register(context, fireAt, bundle)
    }

    private suspend fun getAnchor(ep: ReminderDataEntryPoint, scheduleId: String): Long? =
        ep.tokenDataStore().getPreference(ANCHOR_PREFIX + scheduleId)?.toLongOrNull()

    private fun register(context: Context, fireAt: Long, bundle: Bundle) {
        val scheduleId = bundle.getString(EXTRA_SCHEDULE_ID) ?: return
        val rc = requestCodeFor(scheduleId)
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = pendingIntentFor(context, rc, bundle)
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        // Android 14+ 默认拒 SCHEDULE_EXACT_ALARM：降级为不精确（Doze 下可能延迟数分钟）
        if (exact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pi)
        }
        registeredCodes.add(rc)
        Log.i(TAG, "Alarm $scheduleId @ $fireAt (${if (exact) "exact" else "inexact"})")
    }

    /** 触发后/稍后提醒重排：同一 requestCode 重新注册，extras 更新为当前快照。 */
    private fun reRegister(context: Context, fired: Intent, fireAt: Long) {
        val bundle = fired.extras ?: return
        bundle.putLong(EXTRA_FIRE_AT, fireAt)
        bundle.remove(EXTRA_SNOOZE)
        register(context, fireAt, bundle)
    }

    private fun pendingIntentFor(context: Context, rc: Int, extras: Bundle? = null): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_FIRE
            extras?.let { putExtras(it) }
        }
        return PendingIntent.getBroadcast(context, rc, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun requestCodeFor(scheduleId: String): Int = scheduleId.hashCode() and 0x7FFFFFFF

    private fun buildMedBody(med: MedicationResponse): String {
        val dosage = med.dosage?.takeIf { it.isNotBlank() }
        return if (dosage != null) "${med.name} × $dosage" else med.name
    }

    internal fun entryPoint(context: Context): ReminderDataEntryPoint =
        EntryPointAccessors.fromApplication(context.applicationContext, ReminderDataEntryPoint::class.java)

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReminderDataEntryPoint {
        fun medicationRepository(): MedicationRepository
        fun bmRepository(): BmRepository
        fun tokenDataStore(): TokenDataStore
        fun medicationDao(): MedicationDao
        fun bmRecordDao(): BmRecordDao
    }
}
