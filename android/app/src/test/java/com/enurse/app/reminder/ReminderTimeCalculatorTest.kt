package com.enurse.app.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderTimeCalculatorTest {

    /** 固定基准：2026-08-04 09:00 本地时区（避免依赖实时时钟导致断言不稳定） */
    private val base: Long =
        ZonedDateTime.of(2026, 8, 4, 9, 0, 0, 0, ZoneId.systemDefault()).toInstant().toEpochMilli()
    private val day = 86_400_000L
    private val hour = 3_600_000L

    /** base 所在那一天的 HH:mm 时刻（epoch millis） */
    private fun todayAt(hhmm: String): Long {
        val z = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(base), ZoneId.systemDefault())
        return z.withHour(hhmm.substring(0, 2).toInt())
            .withMinute(hhmm.substring(3, 5).toInt())
            .withSecond(0).withNano(0)
            .toInstant().toEpochMilli()
    }

    // ── nextFixedTime ──

    @Test
    fun fixedTime_picksClosestFuture() {
        val fixed = listOf("08:00", "20:00")
        // 09:00（base）时：08:00 已过 → 今天 20:00
        assertEquals(todayAt("20:00"), ReminderTimeCalculator.nextFixedTime(fixed, base))
        // 21:00 时：两个都过了 → 明天 08:00
        assertEquals(todayAt("08:00") + day, ReminderTimeCalculator.nextFixedTime(fixed, todayAt("20:00") + hour))
    }

    @Test
    fun fixedTime_allPassed_picksTomorrowFirst() {
        assertEquals(todayAt("06:00") + day, ReminderTimeCalculator.nextFixedTime(listOf("06:00"), base))
    }

    @Test
    fun fixedTime_emptyOrInvalid_returnsNull() {
        assertNull(ReminderTimeCalculator.nextFixedTime(emptyList(), base))
        assertNull(ReminderTimeCalculator.nextFixedTime(listOf("25:00", "abc"), base))
        assertNull(ReminderTimeCalculator.nextFixedTime(listOf("8:00"), base)) // 非零填充
    }

    // ── nextInterval ──

    @Test
    fun interval_noAnchor_firstRingInNHours() {
        // 首次设置：从现在起算 N 小时后第一次响（对齐服务端 startTime 语义，设置瞬间不响）
        assertEquals(base + 8 * hour, ReminderTimeCalculator.nextInterval(null, 8.0, base))
    }

    @Test
    fun interval_normal_advancesByHours() {
        val anchor = base - 2 * hour
        assertEquals(anchor + 8 * hour, ReminderTimeCalculator.nextInterval(anchor, 8.0, base))
    }

    @Test
    fun interval_late_shouldCatchUp() {
        val anchor = base - 20 * hour // 错过多次
        assertEquals(base, ReminderTimeCalculator.nextInterval(anchor, 8.0, base))
    }

    @Test
    fun interval_fractionalHours() {
        assertEquals(base + 30 * 60_000L, ReminderTimeCalculator.nextInterval(base, 0.5, base))
    }

    // ── nextBmReminder ──

    @Test
    fun bm_todayFuture_returnsToday() {
        assertEquals(todayAt("23:59"), ReminderTimeCalculator.nextBmReminder("23:59", base))
    }

    @Test
    fun bm_todayPassed_returnsTomorrow() {
        assertEquals(todayAt("06:00") + day, ReminderTimeCalculator.nextBmReminder("06:00", base))
    }

    @Test
    fun bm_invalidFormat_returnsNull() {
        assertNull(ReminderTimeCalculator.nextBmReminder("6:00", base))
        assertNull(ReminderTimeCalculator.nextBmReminder("", base))
    }
}
