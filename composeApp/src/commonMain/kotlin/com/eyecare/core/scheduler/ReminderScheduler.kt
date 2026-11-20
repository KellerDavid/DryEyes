package com.eyecare.core.scheduler

import com.eyecare.core.analytics.AnalyticsEvent
import com.eyecare.core.analytics.AnalyticsTracker
import com.eyecare.core.notification.NotificationManager
import com.eyecare.core.notification.ReminderEvent
import com.eyecare.domain.model.NotificationStatus
import com.eyecare.domain.repository.ReminderRepository
import com.eyecare.domain.repository.ScheduleRepository
import com.eyecare.domain.time.AppClock
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

/**
 * Runs while the app is alive (also when minimised to the tray) and decides when a reminder is due.
 *
 * Instead of one long `delay` it re-evaluates every [tick] and looks at the window since the
 * previous evaluation. This makes it robust to sleep/resume and clock changes:
 * - a slot that became due less than [lateTolerance] ago fires once;
 * - older slots (e.g. the machine was asleep) are dropped, so missed reminders never pile up;
 * - a slot is persisted before it is shown, and only slots after the last persisted one may fire,
 *   so the same occurrence is never shown twice (even across restarts).
 */
class ReminderScheduler(
    private val schedules: ScheduleRepository,
    private val reminders: ReminderRepository,
    private val notifier: NotificationManager,
    private val clock: AppClock,
    private val analytics: AnalyticsTracker,
    private val tick: kotlin.time.Duration = 15.seconds,
    private val lateTolerance: Duration = Duration.ofMinutes(2),
) {
    private val _nextTrigger = MutableStateFlow<Instant?>(null)
    /** Earliest upcoming reminder (regular slot or snooze), for the home screen. */
    val nextTrigger: StateFlow<Instant?> = _nextTrigger.asStateFlow()

    private val wake = Channel<Unit>(Channel.CONFLATED)
    private val mutex = Mutex()
    private var lastEvaluatedAt: Instant? = null

    /** Ask for an immediate re-evaluation (routine saved, reminder snoozed, …). */
    fun refresh() {
        wake.trySend(Unit)
    }

    /** Main loop; cancel the calling coroutine to stop it. */
    suspend fun run() {
        while (true) {
            evaluate()
            withTimeoutOrNull(tick) { wake.receive() }
        }
    }

    suspend fun evaluate() = mutex.withLock {
        val now = clock.now()
        val zone = clock.zone()
        val routine = schedules.getSchedules()
        val from = lastEvaluatedAt ?: now
        lastEvaluatedAt = now

        var shown = false

        // 1. Regular routine slot that became due since the previous evaluation.
        if (now.isAfter(from)) {
            val due = NextTriggerCalculator.latestDue(routine, from, now, zone)
            val lastHandled = reminders.lastHandledSlot()
            val isNew = due != null && (lastHandled == null || due.at.isAfter(lastHandled))
            if (due != null && isNew && Duration.between(due.at, now) <= lateTolerance) {
                reminders.skipAllSnoozed() // a fresh reminder supersedes an older snoozed one
                val id = reminders.recordFired(due.scheduleId, due.at)
                show(ReminderEvent(reminderId = id, slotAt = due.at, isSnoozeRepeat = false))
                shown = true
            }
        }

        // 2. Snoozed reminder whose time has come.
        val snooze = reminders.activeSnooze()
        if (snooze != null && !snooze.nextTriggerAt.isAfter(now)) {
            val late = Duration.between(snooze.nextTriggerAt, now) > lateTolerance
            if (shown || late) {
                reminders.setStatus(snooze.id, NotificationStatus.SKIPPED)
            } else {
                reminders.setStatus(snooze.id, NotificationStatus.FIRED)
                show(ReminderEvent(reminderId = snooze.id, slotAt = snooze.slotAt, isSnoozeRepeat = true))
            }
        }

        // 3. Publish the next upcoming moment.
        val nextRegular = NextTriggerCalculator.next(routine, now, zone)?.at
        val nextSnooze = reminders.activeSnooze()?.nextTriggerAt
        _nextTrigger.value = listOfNotNull(nextRegular, nextSnooze).minOrNull()
    }

    private fun show(event: ReminderEvent) {
        notifier.showReminder(event)
        analytics.track(AnalyticsEvent.ReminderShown)
    }
}
