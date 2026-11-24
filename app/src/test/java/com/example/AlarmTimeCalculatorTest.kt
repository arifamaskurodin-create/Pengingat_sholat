package com.example

import com.example.alarm.AlarmTimeCalculator
import com.example.data.AlarmEntity
import com.example.data.RepeatDays
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class AlarmTimeCalculatorTest {

    @Test
    fun `disabled alarm returns null next trigger`() {
        val alarm = AlarmEntity(hour = 7, minute = 0, isEnabled = false)
        assertNull(AlarmTimeCalculator.nextTriggerMillis(alarm, 1_000_000L))
    }

    @Test
    fun `one-time alarm later today triggers today`() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 24, 6, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val alarm = AlarmEntity(hour = 7, minute = 0, isEnabled = true, repeatDaysMask = 0)
        val trigger = AlarmTimeCalculator.nextTriggerMillis(alarm, now)
        assertNotNull(trigger)

        val expected = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 24, 7, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        assertEquals(expected, trigger)
    }

    @Test
    fun `one-time alarm already passed today rolls to tomorrow`() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 24, 8, 15, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val alarm = AlarmEntity(hour = 7, minute = 0, isEnabled = true, repeatDaysMask = 0)
        val trigger = AlarmTimeCalculator.nextTriggerMillis(alarm, now)

        val expected = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 25, 7, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        assertEquals(expected, trigger)
    }

    @Test
    fun `snoozed alarm overrides regular schedule until snooze expires`() {
        val now = 1_700_000_000_000L
        val snoozeUntil = now + 600_000L
        val alarm = AlarmEntity(
            hour = 7,
            minute = 0,
            isEnabled = true,
            snoozeUntilMillis = snoozeUntil
        )
        assertEquals(snoozeUntil, AlarmTimeCalculator.nextTriggerMillis(alarm, now))
    }

    @Test
    fun `formatTimeParts formats 12h and 24h cleanly`() {
        assertEquals("00:05" to null, AlarmTimeCalculator.formatTimeParts(0, 5, true))
        assertEquals("12:05" to "AM", AlarmTimeCalculator.formatTimeParts(0, 5, false))
        assertEquals("12:30" to "PM", AlarmTimeCalculator.formatTimeParts(12, 30, false))
        assertEquals("07:45" to "PM", AlarmTimeCalculator.formatTimeParts(19, 45, false))
    }

    @Test
    fun `formatRepeatSummary summarizes presets and custom days`() {
        assertEquals("Once", AlarmTimeCalculator.formatRepeatSummary(0))
        assertEquals("Weekdays", AlarmTimeCalculator.formatRepeatSummary(RepeatDays.WEEKDAYS))
        assertEquals("Weekends", AlarmTimeCalculator.formatRepeatSummary(RepeatDays.WEEKENDS))
        assertEquals("Every day", AlarmTimeCalculator.formatRepeatSummary(RepeatDays.EVERY_DAY))
        assertEquals(
            "Mon, Wed, Fri",
            AlarmTimeCalculator.formatRepeatSummary(
                RepeatDays.MONDAY or RepeatDays.WEDNESDAY or RepeatDays.FRIDAY
            )
        )
    }
}
