package com.eyecare.core.scheduler

import com.eyecare.core.analytics.AnalyticsEvent
import com.eyecare.core.analytics.AnalyticsTracker
import com.eyecare.core.notification.NotificationManager
import com.eyecare.core.notification.ReminderEvent
import com.eyecare.domain.model.NotificationStatus.MISSED
import com.eyecare.domain.model.NotificationStatus.SHOWN
import com.eyecare.domain.model.NotificationStatus.SNOOZED
import com.eyecare.domain.model.NotificationStatus.STARTED
import com.eyecare.domain.model.ScheduledNotification
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

/** The upcoming reminder: a regular slot or a snoozed one (then [slotAt] is the original time). */
data class NextReminder(val at: Instant, val isSnooze: Boolean, val bandId: Long, val slotAt: Instant)

data class SchedulerState(
    /** A reminder is waiting for the user (toast shown or queued behind an exercise). */
    val dueNow: Boolean = false,
    val next: NextReminder? = null,
)

/**
 * Runs while the app is alive (also when only the tray icon is visible) and decides when a reminder
 * is due. Each evaluation looks at the window since the previous one, which makes it robust to
 * sleep/resume and clock changes:
 * - if several occurrences became due at once (e.g. after sleep), only the latest is shown and the
 *   rest are recorded as missed, so reminders never pile up;
 * - every occurrence is persisted with its (band, slot) key before it is shown, so the same
 *   occurrence is never shown twice, even across restarts;
 * - a newer reminder supersedes one still waiting for an answer (that one becomes missed).
 */
class ReminderScheduler(
    private val schedules: ScheduleRepository,
    private val reminders: ReminderRepository,
    private val notifier: NotificationManager,
    private val clock: AppClock,
    private val analytics: AnalyticsTracker,
    private val tick: kotlin.time.Duration = 5.seconds,
) {
    private val _state = MutableStateFlow(SchedulerState())
    val state: StateFlow<SchedulerState> = _state.asStateFlow()

    private val wake = Channel<Unit>(Channel.CONFLATED)
    private val mutex = Mutex()
    private var lastCheckedAt: Instant? = null

    private sealed interface Candidate {
        val at: Instant
        data class Regular(val slot: Slot) : Candidate { override val at get() = slot.at }
        data class Snoozed(val row: ScheduledNotification) : Candidate { override val at get() = row.nextTriggerAt }
    }

    /** Ask for an immediate re-evaluation (routine saved, reminder snoozed, …). */
    fun refresh() {
        wake.trySend(Unit)
    }

    /** Reminders left open by a previous run cannot be answered any more. */
    suspend fun recoverAfterRestart() = mutex.withLock {
        val now = clock.now()
        reminders.withStatus(SHOWN, STARTED).forEach { reminders.setStatus(it.id, MISSED) }
        reminders.withStatus(SNOOZED).filter { it.nextTriggerAt.isBefore(now) }.forEach { reminders.setStatus(it.id, MISSED) }
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
        val bands = schedules.getBands()
        val from = lastCheckedAt ?: now
        lastCheckedAt = now

        val candidates = mutableListOf<Candidate>()
        if (now.isAfter(from)) {
            NextTriggerCalculator.slotsBetween(bands, from, now, zone)
                .filter { reminders.find(it.bandId, it.at) == null }
                .forEach { candidates += Candidate.Regular(it) }
        }
        reminders.withStatus(SNOOZED).filter { !it.nextTriggerAt.isAfter(now) }.forEach { candidates += Candidate.Snoozed(it) }

        if (candidates.isNotEmpty()) {
            candidates.sortBy { it.at }
            val latest = candidates.removeAt(candidates.lastIndex)
            candidates.forEach { c ->
                when (c) {
                    is Candidate.Regular -> reminders.record(c.slot.bandId, c.slot.at, MISSED)
                    is Candidate.Snoozed -> reminders.setStatus(c.row.id, MISSED)
                }
            }
            fire(latest)
        }
        publish(now)
    }

    private suspend fun fire(candidate: Candidate) {
        val pending = reminders.withStatus(SHOWN)
        val event = when (candidate) {
            is Candidate.Regular -> {
                val id = reminders.record(candidate.slot.bandId, candidate.slot.at, SHOWN)
                ReminderEvent(id, candidate.slot.bandId, candidate.slot.at, isSnoozeRepeat = false)
            }
            is Candidate.Snoozed -> {
                reminders.setStatus(candidate.row.id, SHOWN)
                ReminderEvent(candidate.row.id, candidate.row.bandId, candidate.row.slotAt, isSnoozeRepeat = true)
            }
        }
        pending.filter { it.id != event.reminderId }.forEach { reminders.setStatus(it.id, MISSED) }
        notifier.showReminder(event)
        analytics.track(AnalyticsEvent.ReminderShown)
    }

    private suspend fun publish(now: Instant) {
        val zone = clock.zone()
        val bands = schedules.getBands()
        val handled = reminders.getSince(now.minus(Duration.ofDays(1))).map { it.bandId to it.slotAt }.toSet()
        val regular = NextTriggerCalculator.next(bands, now, zone, handled)
            ?.let { NextReminder(it.at, isSnooze = false, bandId = it.bandId, slotAt = it.at) }
        val snooze = reminders.withStatus(SNOOZED).minByOrNull { it.nextTriggerAt }
            ?.let { NextReminder(it.nextTriggerAt, isSnooze = true, bandId = it.bandId, slotAt = it.slotAt) }
        _state.value = SchedulerState(
            dueNow = reminders.withStatus(SHOWN).isNotEmpty(),
            next = listOfNotNull(regular, snooze).minByOrNull { it.at },
        )
    }
}
