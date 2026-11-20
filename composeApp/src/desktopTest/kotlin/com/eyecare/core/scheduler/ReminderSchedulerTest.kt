package com.eyecare.core.scheduler

import com.eyecare.core.analytics.AnalyticsEvent
import com.eyecare.core.analytics.NoOpAnalyticsTracker
import com.eyecare.domain.model.NotificationStatus
import com.eyecare.domain.model.UserSettings
import com.eyecare.domain.model.WeeklySchedule
import com.eyecare.domain.usecase.SnoozeReminderUseCase
import com.eyecare.testing.FakeClock
import com.eyecare.testing.FakeReminderRepository
import com.eyecare.testing.FakeScheduleRepository
import com.eyecare.testing.FakeSettingsRepository
import com.eyecare.testing.RecordingAnalytics
import com.eyecare.testing.RecordingNotifier
import com.eyecare.testing.local
import kotlinx.coroutines.test.runTest
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReminderSchedulerTest {

    private val monday = WeeklySchedule(
        dayOfWeek = DayOfWeek.MONDAY,
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(17, 0),
        intervalMinutes = 20,
        enabled = true,
    )

    private val clock = FakeClock(local("2026-11-23T09:55"))
    private val schedules = FakeScheduleRepository(listOf(monday))
    private val reminders = FakeReminderRepository()
    private val notifier = RecordingNotifier()
    private val analytics = RecordingAnalytics()
    private val scheduler = ReminderScheduler(schedules, reminders, notifier, clock, analytics)

    @Test
    fun `fires once when a slot becomes due`() = runTest {
        scheduler.evaluate()
        assertEquals(local("2026-11-23T10:00"), scheduler.nextTrigger.value)
        assertTrue(notifier.shown.isEmpty())

        clock.set("2026-11-23T10:00:10")
        scheduler.evaluate()
        assertEquals(1, notifier.shown.size)
        assertEquals(local("2026-11-23T10:00"), notifier.shown.single().slotAt)
        assertTrue(analytics.events.contains(AnalyticsEvent.ReminderShown))

        // Further ticks do not show the same occurrence again.
        clock.set("2026-11-23T10:00:25")
        scheduler.evaluate()
        assertEquals(1, notifier.shown.size)
        assertEquals(local("2026-11-23T10:20"), scheduler.nextTrigger.value)
    }

    @Test
    fun `no backlog after sleep - missed reminders are dropped`() = runTest {
        scheduler.evaluate()
        // Laptop sleeps from 09:55 to 12:31; slots 10:00 … 12:20 were missed.
        clock.set("2026-11-23T12:31")
        scheduler.evaluate()
        assertTrue(notifier.shown.isEmpty())
        assertEquals(local("2026-11-23T12:40"), scheduler.nextTrigger.value)

        clock.set("2026-11-23T12:40:05")
        scheduler.evaluate()
        assertEquals(1, notifier.shown.size)
    }

    @Test
    fun `wake shortly after a slot still shows exactly one reminder`() = runTest {
        scheduler.evaluate()
        clock.set("2026-11-23T10:01")
        scheduler.evaluate()
        assertEquals(1, notifier.shown.size)
    }

    @Test
    fun `restart does not repeat an already shown slot`() = runTest {
        scheduler.evaluate()
        clock.set("2026-11-23T10:00:05")
        scheduler.evaluate()
        assertEquals(1, notifier.shown.size)

        // New scheduler instance (app restart) sharing the same persisted reminders.
        val restarted = ReminderScheduler(schedules, reminders, notifier, clock, NoOpAnalyticsTracker)
        clock.set("2026-11-23T10:00:30")
        restarted.evaluate()
        // Clock set back (manual change) must not re-fire 10:00 either.
        clock.set("2026-11-23T09:59:59")
        restarted.evaluate()
        clock.set("2026-11-23T10:00:20")
        restarted.evaluate()
        assertEquals(1, notifier.shown.size)
    }

    @Test
    fun `snooze delays the reminder and takes precedence when earlier`() = runTest {
        val settings = FakeSettingsRepository(UserSettings(snoozeMinutes = 5))
        val snooze = SnoozeReminderUseCase(reminders, settings, notifier, scheduler, clock, analytics)

        scheduler.evaluate()
        clock.set("2026-11-23T10:00:05")
        scheduler.evaluate()
        val id = notifier.shown.single().reminderId

        snooze(id)
        assertEquals(1, notifier.dismissed)
        assertEquals(NotificationStatus.SNOOZED, reminders.status(id))
        scheduler.evaluate()
        assertEquals(local("2026-11-23T10:05:05"), scheduler.nextTrigger.value)

        clock.set("2026-11-23T10:05:10")
        scheduler.evaluate()
        assertEquals(2, notifier.shown.size)
        assertTrue(notifier.shown.last().isSnoozeRepeat)
        assertEquals(NotificationStatus.FIRED, reminders.status(id))
    }

    @Test
    fun `a new regular slot supersedes a pending snooze`() = runTest {
        val settings = FakeSettingsRepository(UserSettings(snoozeMinutes = 30))
        val snooze = SnoozeReminderUseCase(reminders, settings, notifier, scheduler, clock, analytics)

        scheduler.evaluate()
        clock.set("2026-11-23T10:00:05")
        scheduler.evaluate()
        val first = notifier.shown.single().reminderId
        snooze(first) // until 10:30, but 10:20 slot comes first

        clock.set("2026-11-23T10:20:05")
        scheduler.evaluate()
        assertEquals(2, notifier.shown.size)
        assertFalse(notifier.shown.last().isSnoozeRepeat)
        assertEquals(NotificationStatus.SKIPPED, reminders.status(first))

        clock.set("2026-11-23T10:30:05")
        scheduler.evaluate()
        assertEquals(2, notifier.shown.size)
    }

    @Test
    fun `snooze missed during sleep is skipped, not shown late`() = runTest {
        val settings = FakeSettingsRepository(UserSettings(snoozeMinutes = 5))
        val snooze = SnoozeReminderUseCase(reminders, settings, notifier, scheduler, clock, analytics)
        scheduler.evaluate()
        clock.set("2026-11-23T10:00:05")
        scheduler.evaluate()
        val id = notifier.shown.single().reminderId
        snooze(id)

        clock.advance(Duration.ofMinutes(15)) // asleep 10:00 → 10:15
        scheduler.evaluate()
        assertEquals(1, notifier.shown.size)
        assertEquals(NotificationStatus.SKIPPED, reminders.status(id))
    }

    @Test
    fun `routine change is picked up on next evaluation`() = runTest {
        scheduler.evaluate()
        schedules.replaceSchedules(listOf(monday.copy(intervalMinutes = 5, startTime = LocalTime.of(9, 0))))
        scheduler.evaluate()
        assertEquals(local("2026-11-23T10:00"), scheduler.nextTrigger.value)
        schedules.replaceSchedules(listOf(monday.copy(enabled = false)))
        scheduler.evaluate()
        assertEquals(null, scheduler.nextTrigger.value)
    }
}
