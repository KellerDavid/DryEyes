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
import com.eyecare.domain.model.NotificationStatus
import com.eyecare.domain.model.UserSettings
import com.eyecare.testing.band
import com.eyecare.testing.local
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import java.time.DayOfWeek
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
    fun `seed inserts the 3-step exercise exactly once`() = runBlocking<Unit> {
        DefaultData.seedIfEmpty(exercises)
        DefaultData.seedIfEmpty(exercises)
        assertEquals(1, exercises.countExercises())
        val exercise = exercises.getDefaultExercise()!!
        assertEquals(listOf("Nézz távolra", "Csukd be a szemed", "Pislogj tudatosan"), exercise.steps.map { it.title })
        assertEquals(45, exercise.totalSeconds)
    }

    @Test
    fun `bands round-trip including days and midnight crossing`() = runBlocking<Unit> {
        val id = schedules.saveBand(band(id = 0, days = setOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY), start = "22:00", end = "02:00", interval = 120))
        val saved = schedules.getBands().single()
        assertEquals(id, saved.id)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY), saved.days)
        assertEquals(true, saved.crossesMidnight)
        schedules.saveBand(saved.copy(active = false))
        assertEquals(false, schedules.getBands().single().active)
        schedules.deleteBand(id)
        assertEquals(emptyList(), schedules.getBands())
    }

    @Test
    fun `occurrences are unique per band and slot`() = runBlocking<Unit> {
        val id = reminders.record(1, local("2026-11-23T10:30"), NotificationStatus.SHOWN)
        assertFailsWith<Exception> { reminders.record(1, local("2026-11-23T10:30"), NotificationStatus.SHOWN) }
        reminders.snooze(id, local("2026-11-23T10:40"))
        val snoozed = reminders.withStatus(NotificationStatus.SNOOZED).single()
        assertEquals(true, snoozed.wasSnoozed)
        assertEquals(local("2026-11-23T10:40"), snoozed.nextTriggerAt)
        reminders.setStatus(id, NotificationStatus.DONE)
        assertEquals(NotificationStatus.DONE, reminders.find(1, local("2026-11-23T10:30"))?.status)
        reminders.delete(id)
        assertNull(reminders.find(1, local("2026-11-23T10:30")))
    }

    @Test
    fun `settings default and persist`() = runBlocking<Unit> {
        assertEquals(UserSettings(), settings.getSettings())
        settings.saveSettings(UserSettings(snoozeMinutes = 15, setupAt = local("2026-11-20T08:00")))
        assertEquals(UserSettings(15, local("2026-11-20T08:00")), settings.observeSettings().first())
    }

    @Test
    fun `completions keep the reminder link`() = runBlocking<Unit> {
        DefaultData.seedIfEmpty(exercises)
        val id = exercises.getDefaultExercise()!!.id
        exercises.recordCompletion(id, null, local("2026-11-23T10:00"))
        exercises.recordCompletion(id, 5, local("2026-11-23T11:00"))
        assertEquals(listOf(null, 5L), exercises.observeCompletionsSince(local("2026-11-23T00:00")).first().map { it.reminderId })
    }

    @Test
    fun `file database keeps data across reopen`() = runBlocking<Unit> {
        val file = File(Files.createTempDirectory("dryeyes").toFile(), "test.db")
        DatabaseFactory.create(file).also {
            RoomSettingsRepository(it).saveSettings(UserSettings(snoozeMinutes = 5))
            it.close()
        }
        DatabaseFactory.create(file).also {
            assertEquals(5, RoomSettingsRepository(it).getSettings().snoozeMinutes)
            it.close()
        }
    }
}
