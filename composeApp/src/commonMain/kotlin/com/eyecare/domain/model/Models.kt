package com.eyecare.domain.model

import java.time.DayOfWeek
import java.time.Instant

/**
 * One time band of the weekly routine ("idősáv"): on each of [days], remind every [intervalMinutes]
 * from [startMinute]. If [endMinute] <= [startMinute] the band crosses midnight and ends the next day.
 */
data class TimeBand(
    val id: Long = 0,
    val days: Set<DayOfWeek>,
    val startMinute: Int,
    val endMinute: Int,
    val intervalMinutes: Int,
    val active: Boolean,
) {
    val crossesMidnight: Boolean get() = endMinute <= startMinute

    /** Length in minutes; equal start and end means a full day. */
    val durationMinutes: Int get() = ((endMinute - startMinute) + MINUTES_PER_DAY) % MINUTES_PER_DAY
        .let { if (it == 0) MINUTES_PER_DAY else it }

    val reminderCount: Int get() = (durationMinutes + intervalMinutes - 1) / intervalMinutes

    companion object {
        const val MINUTES_PER_DAY = 1440
        val INTERVAL_OPTIONS = listOf(60, 90, 120, 180)
        const val TIME_STEP_MINUTES = 30

        val DEFAULT = TimeBand(
            days = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY),
            startMinute = 9 * 60,
            endMinute = 17 * 60,
            intervalMinutes = 90,
            active = true,
        )
    }
}

enum class NotificationStatus {
    /** Shown (toast pending or queued behind an exercise), not acted on yet. */
    SHOWN,
    SNOOZED,
    /** Exercise started from this reminder and still running. */
    STARTED,
    DONE,
    /** Dismissed, superseded by a newer reminder, or skipped while catching up. */
    MISSED,
    ABORTED,
}

/** State of one routine occurrence, identified by ([bandId], [slotAt]). */
data class ScheduledNotification(
    val id: Long = 0,
    val bandId: Long,
    val slotAt: Instant,
    val nextTriggerAt: Instant,
    val status: NotificationStatus,
    val wasSnoozed: Boolean,
)

data class ExerciseStep(
    val id: Long = 0,
    val title: String,
    val instruction: String,
    val durationSeconds: Int,
    val order: Int,
    /** Which illustration to draw: "far", "closed", "blink". */
    val art: String,
)

data class Exercise(
    val id: Long = 0,
    val name: String,
    val description: String,
    val steps: List<ExerciseStep>,
) {
    val totalSeconds: Int get() = steps.sumOf { it.durationSeconds }
}

/** [reminderId] is null for an exercise started outside the routine ("soron kívüli"). */
data class ExerciseCompletion(
    val id: Long = 0,
    val exerciseId: Long,
    val reminderId: Long?,
    val completedAt: Instant,
)

data class UserSettings(
    val snoozeMinutes: Int = DEFAULT_SNOOZE_MINUTES,
    /** When the first routine was saved (onboarding); null until then. */
    val setupAt: Instant? = null,
) {
    companion object {
        const val DEFAULT_SNOOZE_MINUTES = 10
        val SNOOZE_OPTIONS = listOf(5, 10, 15)
    }
}
