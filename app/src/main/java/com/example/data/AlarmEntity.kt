package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val isEnabled: Boolean = true,
    val repeatDaysMask: Int = 0,
    val ringtoneUri: String = "",
    val ringtoneTitle: String = "Default system alarm",
    val vibrate: Boolean = true,
    val gradualWake: Boolean = true,
    val snoozeMinutes: Int = 10,
    val snoozeUntilMillis: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    val isRepeating: Boolean
        get() = repeatDaysMask != 0

    fun repeatsOn(dayBit: Int): Boolean = RepeatDays.isDaySelected(repeatDaysMask, dayBit)

    fun selectedDayBits(): Set<Int> = RepeatDays.orderedBits
        .map { it.first }
        .filter { RepeatDays.isDaySelected(repeatDaysMask, it) }
        .toSet()
}

object RepeatDays {
    // Bitmask order: 0 = Monday .. 6 = Sunday (matches DayOfWeek ordering)
    const val MONDAY = 1 shl 0
    const val TUESDAY = 1 shl 1
    const val WEDNESDAY = 1 shl 2
    const val THURSDAY = 1 shl 3
    const val FRIDAY = 1 shl 4
    const val SATURDAY = 1 shl 5
    const val SUNDAY = 1 shl 6

    const val WEEKDAYS = MONDAY or TUESDAY or WEDNESDAY or THURSDAY or FRIDAY
    const val WEEKENDS = SATURDAY or SUNDAY
    const val EVERY_DAY = WEEKDAYS or WEEKENDS

    val orderedBits = listOf(
        MONDAY to "M",
        TUESDAY to "T",
        WEDNESDAY to "W",
        THURSDAY to "T",
        FRIDAY to "F",
        SATURDAY to "S",
        SUNDAY to "S"
    )

    val orderedShortNames = listOf(
        MONDAY to "Mon",
        TUESDAY to "Tue",
        WEDNESDAY to "Wed",
        THURSDAY to "Thu",
        FRIDAY to "Fri",
        SATURDAY to "Sat",
        SUNDAY to "Sun"
    )

    fun isDaySelected(mask: Int, bit: Int): Boolean = (mask and bit) != 0

    fun toggleDay(mask: Int, bit: Int): Int = mask xor bit

    fun calendarDayToBit(calendarDayOfWeek: Int): Int = when (calendarDayOfWeek) {
        java.util.Calendar.MONDAY -> MONDAY
        java.util.Calendar.TUESDAY -> TUESDAY
        java.util.Calendar.WEDNESDAY -> WEDNESDAY
        java.util.Calendar.THURSDAY -> THURSDAY
        java.util.Calendar.FRIDAY -> FRIDAY
        java.util.Calendar.SATURDAY -> SATURDAY
        java.util.Calendar.SUNDAY -> SUNDAY
        else -> MONDAY
    }
}
