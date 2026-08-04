package com.enurse.app.reminder

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * 纯 JVM 触发时间计算（无 Android 依赖，可单测）。
 *
 * 所有结果都是绝对 epoch millis（RTC_WAKEUP 使用），时区/夏令时由每次现算天然处理。
 */
object ReminderTimeCalculator {

    private val HHMM = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")

    fun parseHhMm(raw: String): LocalTime? = try {
        if (!HHMM.matches(raw)) null else LocalTime.parse(raw)
    } catch (e: Exception) {
        null
    }

    /**
     * 固定时刻提醒：取今天里最接近的未来时刻；今天全部已过则取明天的第一个。
     * 空列表或全部解析失败返回 null（该 schedule 不注册闹钟）。
     */
    fun nextFixedTime(fixedTimes: List<String>, now: Long): Long? {
        val times = fixedTimes.mapNotNull { parseHhMm(it) }.sorted()
        if (times.isEmpty()) return null
        val todayCandidates = times.map { epochOfDayOffset(it, now, 0) }
        return todayCandidates.filter { it > now }.minOrNull()
            ?: epochOfDayOffset(times.first(), now, 1)
    }

    /**
     * 间隔提醒：从 lastAlarmAt（上次响铃锚点）起算 + N 小时。
     * 锚点为 null（首次设置）→ 从现在起算，N 小时后第一次响（对齐服务端 startTime 语义，
     * 设置瞬间不响）；结果已过期（关机/勿扰错过）→ 补响一次（now）。
     * 注意：调用方必须过滤 intervalHours <= 0 的 schedule，否则会无限触发。
     */
    fun nextInterval(lastAlarmAt: Long?, intervalHours: Double, now: Long): Long {
        val base = lastAlarmAt ?: now
        val next = base + (intervalHours * 3_600_000L).toLong()
        return if (next <= now) now else next
    }

    /**
     * 大便提醒：今天 reminderTime，已过则明天。格式非法返回 null。
     */
    fun nextBmReminder(reminderTime: String, now: Long): Long? {
        val t = parseHhMm(reminderTime) ?: return null
        val today = epochOfDayOffset(t, now, 0)
        return if (today > now) today else epochOfDayOffset(t, now, 1)
    }

    /** now 所在时区的「今天 + dayOffset 天」的 time 时刻，换算成 epoch millis（DST 安全）。 */
    private fun epochOfDayOffset(time: LocalTime, now: Long, dayOffset: Long): Long {
        val zoned = ZonedDateTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.systemDefault())
        return zoned.toLocalDate().plusDays(dayOffset).atTime(time)
            .atZone(zoned.zone).toInstant().toEpochMilli()
    }
}
