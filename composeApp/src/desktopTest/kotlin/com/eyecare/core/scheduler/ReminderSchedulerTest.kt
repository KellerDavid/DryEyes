package com.eyecare.core.scheduler

import com.eyecare.core.analytics.AnalyticsEvent
import com.eyecare.core.analytics.NoOpAnalyticsTracker
import com.eyecare.domain.model.NotificationStatus
import com.eyecare.domain.model.UserSettings
import com.eyecare.domain.usecase.DismissReminderUseCase
import com.eyecare.domain.usecase.SnoozeReminderUseCase
import com.eyecare.domain.usecase.StartFromReminderUseCase
import com.eyecare.testing.FakeClock
import com.eyecare.testing.FakeReminderRepository
import com.eyecare.testing.FakeScheduleRepository
import com.eyecare.testing.FakeSettingsRepository
import com.eyecare.testing.RecordingAnalytics
import com.eyecare.testing.RecordingNotifier
import com.eyecare.testing.band
import com.eyecare.testing.local
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReminderSchedulerTest {

    private val clock = FakeClock(local("2026-11-23T08:55"))
    private val schedules = FakeScheduleRepository(listOf(band()))
    private val reminders = FakeReminderRepository()
    private val notifier = RecordingNotifier()
    private val analytics = RecordingAnalytics()
    private val scheduler = ReminderScheduler(schedules, reminders, notifier, clock, analytics)
    private val settings = FakeSettingsRepository(UserSettings(snoozeMinutes = 10))
    private val snooze = SnoozeReminderUseCase(reminders, settings, notifier, scheduler, clock, analytics)

    private suspend fun at(time: String) {
        clock.set(time)
        scheduler.evaluate()
    }

    @Test
    fun `fires once when a slot becomes due`() = runTest {
        at("2026-11-23T08:55")
        assertEquals(local("2026-11-23T09:00"), scheduler.state.value.next?.at)
        assertFalse(scheduler.state.value.dueNow)

        at("2026-11-23T09:00:02")
        assertEquals(1, notifier.shown.size)
        assertTrue(scheduler.state.value.dueNow)
        assertTrue(analytics.events.contains(AnalyticsEvent.ReminderShown))

        at("2026-11-23T09:00:20")
        assertEquals(1, notifier.shown.size)
    }

    @Test
    fun `after sleep only the latest occurrence is shown, earlier ones are missed`() = runTest {
        at("2026-11-23T08:55")
        at("2026-11-23T12:05") // asleep: 09:00 and 10:30 missed, 12:00 shown
        assertEquals(1, notifier.shown.size)
        assertEquals(local("2026-11-23T12:00"), notifier.shown.single().slotAt)
        assertEquals(NotificationStatus.MISSED, reminders.statusAt(local("2026-11-23T09:00")))
        assertEquals(NotificationStatus.MISSED, reminders.statusAt(local("2026-11-23T10:30")))
        assertEquals(NotificationStatus.SHOWN, reminders.statusAt(local("2026-11-23T12:00")))
    }

    @Test
    fun `slots before the app started are not replayed`() = runTest {
        at("2026-11-23T11:00") // first evaluation of this run
        assertTrue(notifier.shown.isEmpty())
        assertEquals(local("2026-11-23T12:00"), scheduler.state.value.next?.at)
    }

    @Test
    fun `a newer reminder supersedes an unanswered one`() = runTest {
        at("2026-11-23T08:55")
        at("2026-11-23T09:00:01")
        at("2026-11-23T10:30:01")
        assertEquals(2, notifier.shown.size)
        assertEquals(NotificationStatus.MISSED, reminders.statusAt(local("2026-11-23T09:00")))
    }

    @Test
    fun `reminder answered with an exercise is not superseded`() = runTest {
        val start = StartFromReminderUseCase(reminders, scheduler)
        at("2026-11-23T08:55")
        at("2026-11-23T09:00:01")
        start(notifier.shown.single().reminderId)
        at("2026-11-23T10:30:01")
        assertEquals(NotificationStatus.STARTED, reminders.statusAt(local("2026-11-23T09:00")))
    }

    @Test
    fun `restart does not repeat an occurrence`() = runTest {
        at("2026-11-23T08:55")
        at("2026-11-23T09:00:05")
        val restarted = ReminderScheduler(schedules, reminders, notifier, clock, NoOpAnalyticsTracker)
        restarted.recoverAfterRestart()
        assertEquals(NotificationStatus.MISSED, reminders.statusAt(local("2026-11-23T09:00")))
        clock.set("2026-11-23T08:59:59") // clock set back manually
        restarted.evaluate()
        clock.set("2026-11-23T09:00:30")
        restarted.evaluate()
        assertEquals(1, notifier.shown.size)
    }

    @Test
    fun `snooze shows the reminder again after the snooze time`() = runTest {
        at("2026-11-23T08:55")
        at("2026-11-23T09:00:05")
        val id = notifier.shown.single().reminderId
        snooze(id)
        assertEquals(1, notifier.dismissed)
        at("2026-11-23T09:00:10")
        val next = scheduler.state.value.next!!
        assertTrue(next.isSnooze)
        assertEquals(local("2026-11-23T09:10:05"), next.at)
        assertEquals(local("2026-11-23T09:00"), next.slotAt)
        assertFalse(scheduler.state.value.dueNow)

        at("2026-11-23T09:10:06")
        assertEquals(2, notifier.shown.size)
        assertTrue(notifier.shown.last().isSnoozeRepeat)
        assertEquals(id, notifier.shown.last().reminderId)
        assertTrue(reminders.rows.value.single { it.id == id }.wasSnoozed)
    }

    @Test
    fun `dismiss marks the occurrence missed`() = runTest {
        val dismiss = DismissReminderUseCase(reminders, notifier, scheduler, analytics)
        at("2026-11-23T08:55")
        at("2026-11-23T09:00:05")
        dismiss(notifier.shown.single().reminderId)
        at("2026-11-23T09:00:06")
        assertEquals(NotificationStatus.MISSED, reminders.statusAt(local("2026-11-23T09:00")))
        assertFalse(scheduler.state.value.dueNow)
        assertEquals(local("2026-11-23T10:30"), scheduler.state.value.next?.at)
    }

    @Test
    fun `routine change is picked up`() = runTest {
        at("2026-11-23T08:55")
        schedules.saveBand(band(active = false))
        at("2026-11-23T08:56")
        assertEquals(null, scheduler.state.value.next)
    }
}
