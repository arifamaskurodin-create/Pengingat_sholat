package com.example.alarm

import com.example.data.AlarmEntity
import com.example.data.RepeatDays
import java.util.Calendar
import java.util.Locale
import kotlin.math.max

object AlarmTimeCalculator {

    fun nextTriggerMillis(alarm: AlarmEntity, nowMillis: Long = System.currentTimeMillis()): Long? {
        if (!alarm.isEnabled) return null

        val snoozeUntil = alarm.snoozeUntilMillis
        if (snoozeUntil != null && snoozeUntil > nowMillis) {
            return snoozeUntil
        }

        val candidate = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (alarm.repeatDaysMask == 0) {
            if (candidate.timeInMillis <= nowMillis) {
                candidate.add(Calendar.DAY_OF_YEAR, 1)
            }
            return candidate.timeInMillis
        }

        for (dayOffset in 0..7) {
            val checkCal = Calendar.getInstance().apply {
                timeInMillis = candidate.timeInMillis
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }
            val dayBit = RepeatDays.calendarDayToBit(checkCal.get(Calendar.DAY_OF_WEEK))
            if (RepeatDays.isDaySelected(alarm.repeatDaysMask, dayBit) && checkCal.timeInMillis > nowMillis) {
                return checkCal.timeInMillis
            }
        }
        return null
    }

    fun findNextAlarm(
        alarms: List<AlarmEntity>,
        nowMillis: Long = System.currentTimeMillis()
    ): Pair<AlarmEntity, Long>? {
        return alarms
            .mapNotNull { alarm ->
                nextTriggerMillis(alarm, nowMillis)?.let { trigger -> alarm to trigger }
            }
            .minByOrNull { it.second }
    }

    fun formatCountdown(
        targetMillis: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        val diffMillis = max(0L, targetMillis - nowMillis)
        val totalMinutes = ((diffMillis + 59_999L) / 60_000L).toInt()
        if (totalMinutes <= 1) return "in less than a minute"
        val days = totalMinutes / (24 * 60)
        val hours = (totalMinutes % (24 * 60)) / 60
        val minutes = totalMinutes % 60
        return buildString {
            append("in ")
            if (days > 0) append("${days}d ")
            if (hours > 0) append("${hours}h ")
            if (minutes > 0 || (days == 0 && hours == 0)) append("${minutes}m")
        }.trim()
    }

    fun formatTimeParts(hour24: Int, minute: Int, is24Hour: Boolean): Pair<String, String?> {
        return if (is24Hour) {
            String.format(Locale.US, "%02d:%02d", hour24, minute) to null
        } else {
            val period = if (hour24 < 12) "AM" else "PM"
            val hour12 = when (val h = hour24 % 12) {
                0 -> 12
                else -> h
            }
            String.format(Locale.US, "%02d:%02d", hour12, minute) to period
        }
    }

    fun formatRepeatSummary(repeatDaysMask: Int): String {
        return when (repeatDaysMask) {
            0 -> "Once"
            RepeatDays.EVERY_DAY -> "Every day"
            RepeatDays.WEEKDAYS -> "Weekdays"
            RepeatDays.WEEKENDS -> "Weekends"
            else -> RepeatDays.orderedShortNames
                .filter { (bit, _) -> RepeatDays.isDaySelected(repeatDaysMask, bit) }
                .joinToString(", ") { it.second }
        }
    }

    fun formatTriggerDayLabel(triggerMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val targetCal = Calendar.getInstance().apply { timeInMillis = triggerMillis }
        return if (
            nowCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
            nowCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)
        ) {
            "Today"
        } else {
            val tomorrow = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                add(Calendar.DAY_OF_YEAR, 1)
            }
            if (
                tomorrow.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
                tomorrow.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)
            ) {
                "Tomorrow"
            } else {
                android.text.format.DateFormat.format("EEE, MMM d", targetCal).toString()
            }
        }
    }
}
