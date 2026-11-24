package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [AlarmEntity::class], version = 2, exportSchema = false)
abstract class AlarmDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    companion object {
        @Volatile
        private var INSTANCE: AlarmDatabase? = null

        fun getInstance(context: Context): AlarmDatabase {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                lateinit var db: AlarmDatabase
                db = Room.databaseBuilder(
                    appContext,
                    AlarmDatabase::class.java,
                    "calm_alarm_db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .addCallback(object : Callback() {
                        override fun onCreate(database: SupportSQLiteDatabase) {
                            super.onCreate(database)
                            CoroutineScope(Dispatchers.IO).launch {
                                val dao = db.alarmDao()
                                if (dao.getAllAlarmsOnce().isEmpty()) {
                                    val defaultPrayers = listOf(
                                        AlarmEntity(
                                            hour = 4,
                                            minute = 35,
                                            label = "Subuh",
                                            isEnabled = true,
                                            repeatDaysMask = RepeatDays.EVERY_DAY,
                                            vibrate = true,
                                            gradualWake = true,
                                            snoozeMinutes = 5
                                        ),
                                        AlarmEntity(
                                            hour = 11,
                                            minute = 55,
                                            label = "Dzuhur",
                                            isEnabled = true,
                                            repeatDaysMask = RepeatDays.EVERY_DAY,
                                            vibrate = true,
                                            gradualWake = true,
                                            snoozeMinutes = 5
                                        ),
                                        AlarmEntity(
                                            hour = 15,
                                            minute = 15,
                                            label = "Ashar",
                                            isEnabled = true,
                                            repeatDaysMask = RepeatDays.EVERY_DAY,
                                            vibrate = true,
                                            gradualWake = true,
                                            snoozeMinutes = 5
                                        ),
                                        AlarmEntity(
                                            hour = 17,
                                            minute = 55,
                                            label = "Maghrib",
                                            isEnabled = true,
                                            repeatDaysMask = RepeatDays.EVERY_DAY,
                                            vibrate = true,
                                            gradualWake = true,
                                            snoozeMinutes = 5
                                        ),
                                        AlarmEntity(
                                            hour = 19,
                                            minute = 5,
                                            label = "Isya",
                                            isEnabled = true,
                                            repeatDaysMask = RepeatDays.EVERY_DAY,
                                            vibrate = true,
                                            gradualWake = true,
                                            snoozeMinutes = 5
                                        )
                                    )
                                    defaultPrayers.forEach { dao.insertAlarm(it) }
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = db
                db
            }
        }
    }
}
