package com.example

import com.example.data.AlarmDao
import com.example.data.AlarmEntity
import com.example.data.AlarmRepository
import com.example.data.AlarmScheduleCoordinator
import com.example.data.RepeatDays
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmRepositoryTest {

    private class FakeAlarmDao : AlarmDao {
        private val alarms = MutableStateFlow<List<AlarmEntity>>(emptyList())
        private var nextId = 1L

        override fun observeAlarms(): Flow<List<AlarmEntity>> =
            alarms.map { list -> list.sortedWith(compareBy({ it.hour }, { it.minute }, { it.id })) }

        override fun observeEnabledAlarms(): Flow<List<AlarmEntity>> =
            observeAlarms().map { list -> list.filter { it.isEnabled } }

        override suspend fun getAllAlarmsOnce(): List<AlarmEntity> = alarms.value

        override suspend fun getEnabledAlarmsOnce(): List<AlarmEntity> =
            alarms.value.filter { it.isEnabled }

        override fun observeAlarmById(id: Long): Flow<AlarmEntity?> =
            alarms.map { list -> list.firstOrNull { it.id == id } }

        override suspend fun getAlarmById(id: Long): AlarmEntity? =
            alarms.value.firstOrNull { it.id == id }

        override suspend fun insertAlarm(alarm: AlarmEntity): Long {
            val assignedId = if (alarm.id == 0L) nextId++ else alarm.id
            val entity = alarm.copy(id = assignedId)
            alarms.value = alarms.value.filterNot { it.id == assignedId } + entity
            return assignedId
        }

        override suspend fun updateAlarm(alarm: AlarmEntity) {
            alarms.value = alarms.value.map { if (it.id == alarm.id) alarm else it }
        }

        override suspend fun deleteAlarmById(id: Long) {
            alarms.value = alarms.value.filterNot { it.id == id }
        }

        override suspend fun deleteAllAlarms() {
            alarms.value = emptyList()
        }
    }

    private class RecordingScheduler : AlarmScheduleCoordinator {
        val scheduledIds = mutableListOf<Long>()
        val canceledIds = mutableListOf<Long>()

        override fun schedule(alarm: AlarmEntity) {
            scheduledIds.add(alarm.id)
        }

        override fun cancel(alarmId: Long) {
            canceledIds.add(alarmId)
        }
    }

    @Test
    fun `createAlarm persists time label and repeat schedule and schedules alarm`() = runTest {
        val dao = FakeAlarmDao()
        val scheduler = RecordingScheduler()
        val repo = AlarmRepository(dao, scheduler)

        val created = repo.createAlarm(
            hour = 6,
            minute = 45,
            label = "  Morning Run  ",
            repeatDaysMask = RepeatDays.WEEKDAYS
        )

        assertEquals(6, created.hour)
        assertEquals(45, created.minute)
        assertEquals("Morning Run", created.label)
        assertEquals(RepeatDays.WEEKDAYS, created.repeatDaysMask)
        assertTrue(created.isRepeating)
        assertEquals(listOf(created.id), scheduler.scheduledIds)

        val observed = repo.alarmsFlow.first()
        assertEquals(1, observed.size)
        assertEquals(created, observed.first())
    }

    @Test
    fun `updateTime updateLabel and updateRepeatSchedule mutate persisted alarm`() = runTest {
        val dao = FakeAlarmDao()
        val scheduler = RecordingScheduler()
        val repo = AlarmRepository(dao, scheduler)

        val created = repo.createAlarm(hour = 7, minute = 0, label = "Wake")
        repo.updateTime(created.id, hour = 8, minute = 15)
        repo.updateLabel(created.id, "Deep Work")
        repo.updateRepeatSchedule(created.id, RepeatDays.WEEKENDS)

        val updated = repo.getAlarmById(created.id)
        assertNotNull(updated)
        assertEquals(8, updated?.hour)
        assertEquals(15, updated?.minute)
        assertEquals("Deep Work", updated?.label)
        assertEquals(RepeatDays.WEEKENDS, updated?.repeatDaysMask)
    }

    @Test
    fun `toggle and dismiss handle one-time vs repeating alarms properly`() = runTest {
        val dao = FakeAlarmDao()
        val scheduler = RecordingScheduler()
        val repo = AlarmRepository(dao, scheduler)

        val oneShot = repo.createAlarm(hour = 9, minute = 0, repeatDaysMask = 0)
        val repeating = repo.createAlarm(hour = 7, minute = 30, repeatDaysMask = RepeatDays.WEEKDAYS)

        repo.toggleAlarm(oneShot, false)
        assertFalse(repo.getAlarmById(oneShot.id)!!.isEnabled)
        assertTrue(scheduler.canceledIds.contains(oneShot.id))

        repo.toggleAlarm(oneShot, true)
        repo.dismissAlarm(oneShot.id)
        assertFalse(repo.getAlarmById(oneShot.id)!!.isEnabled)

        repo.dismissAlarm(repeating.id)
        assertTrue(repo.getAlarmById(repeating.id)!!.isEnabled)
    }

    @Test
    fun `deleteAlarmById removes alarm and cancels schedule`() = runTest {
        val dao = FakeAlarmDao()
        val scheduler = RecordingScheduler()
        val repo = AlarmRepository(dao, scheduler)

        val alarm = repo.createAlarm(hour = 10, minute = 0, label = "Standup")
        repo.deleteAlarmById(alarm.id)

        assertNull(repo.getAlarmById(alarm.id))
        assertTrue(scheduler.canceledIds.contains(alarm.id))
        assertTrue(repo.alarmsFlow.first().isEmpty())
    }
}
