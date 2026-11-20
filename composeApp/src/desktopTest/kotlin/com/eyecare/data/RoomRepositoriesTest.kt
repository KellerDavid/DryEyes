package com.eyecare.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.eyecare.data.database.AppDatabase
import com.eyecare.data.datasource.DefaultData
import com.eyecare.data.repository.RoomExerciseRepository
import com.eyecare.data.repository.RoomReminderRepository
import com.eyecare.data.repository.RoomScheduleRepository
import com.eyecare.data.repository.RoomSettingsRepository
import com.eyecare.database.DatabaseFactory
import com.eyecare.domain.model.AppTheme
import com.eyecare.domain.model.NotificationStatus
import com.eyecare.domain.model.UserSettings
import com.eyecare.testing.local
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import java.time.DayOfWeek
import java.time.LocalTime
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RoomRepositoriesTest {

    private val db = Room.inMemoryDatabaseBuilder<AppDatabase>()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()

    private val schedules = RoomScheduleRepository(db)
    private val reminders = RoomReminderRepository(db)
    private val exercises = RoomExerciseRepository(db)
    private val settings = RoomSettingsRepository(db)

    @AfterTest
    fun close() = db.close()

    @Test
    fun `seed inserts default exercise and routine exactly once`() = runBlocking<Unit> {
        DefaultData.seedIfEmpty(exercises, schedules)
        DefaultData.seedIfEmpty(exercises, schedules)
        assertEquals(1, exercises.countExercises())
        val exercise = exercises.getDefaultExercise()!!
        assertEquals(DefaultData.defaultExercise.steps.map { it.instruction }, exercise.steps.map { it.instruction })
        val routine = schedules.getSchedules()
        assertEquals(7, routine.size)
        assertEquals(5, routine.count { it.enabled })
        assertEquals(LocalTime.of(9, 0), routine.first { it.dayOfWeek == DayOfWeek.MONDAY }.startTime)
    }

    @Test
    fun `reminder lifecycle round-trips`() = runBlocking<Unit> {
        val id = reminders.recordFired(1, local("2026-11-23T10:00"))
        assertEquals(local("2026-11-23T10:00"), reminders.lastHandledSlot())
        reminders.snooze(id, local("2026-11-23T10:10"))
        assertEquals(local("2026-11-23T10:10"), reminders.activeSnooze()?.nextTriggerAt)
        reminders.skipAllSnoozed()
        assertNull(reminders.activeSnooze())
        reminders.setStatus(id, NotificationStatus.DONE)
    }

    @Test
    fun `settings default and persist`() = runBlocking<Unit> {
        assertEquals(UserSettings(), settings.getSettings())
        settings.saveSettings(UserSettings(theme = AppTheme.DARK, snoozeMinutes = 15))
        assertEquals(UserSettings(AppTheme.DARK, 15), settings.observeSettings().first())
    }

    @Test
    fun `completions are stored`() = runBlocking<Unit> {
        DefaultData.seedIfEmpty(exercises, schedules)
        val id = exercises.getDefaultExercise()!!.id
        exercises.recordCompletion(id, local("2026-11-23T10:00"))
        assertEquals(1, exercises.observeCompletions().first().size)
    }

    @Test
    fun `file database keeps data across reopen`() = runBlocking<Unit> {
        val file = File(Files.createTempDirectory("dryeyes").toFile(), "test.db")
        DatabaseFactory.create(file).also {
            RoomSettingsRepository(it).saveSettings(UserSettings(snoozeMinutes = 30))
            it.close()
        }
        DatabaseFactory.create(file).also {
            assertEquals(30, RoomSettingsRepository(it).getSettings().snoozeMinutes)
            it.close()
        }
    }
}
