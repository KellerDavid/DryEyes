package com.eyecare.domain.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime

/** One active window of the weekly routine: on [dayOfWeek], remind every [intervalMinutes] between [startTime] and [endTime]. */
data class WeeklySchedule(
    val id: Long = 0,
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val intervalMinutes: Int,
    val enabled: Boolean,
)

enum class NotificationStatus { FIRED, SNOOZED, DONE, SKIPPED }

/** A reminder occurrence that was actually shown (or snoozed) for a given routine [slotAt]. */
data class ScheduledNotification(
    val id: Long = 0,
    val scheduleId: Long?,
    val slotAt: Instant,
    val nextTriggerAt: Instant,
    val status: NotificationStatus,
)

data class ExerciseStep(
    val id: Long = 0,
    val instruction: String,
    val durationSeconds: Int,
    val order: Int,
)

data class Exercise(
    val id: Long = 0,
    val name: String,
    val description: String,
    val steps: List<ExerciseStep>,
) {
    val totalSeconds: Int get() = steps.sumOf { it.durationSeconds }
}

data class ExerciseCompletion(
    val id: Long = 0,
    val exerciseId: Long,
    val completedAt: Instant,
)

enum class AppTheme { SYSTEM, LIGHT, DARK }

data class UserSettings(
    val theme: AppTheme = AppTheme.SYSTEM,
    val snoozeMinutes: Int = DEFAULT_SNOOZE_MINUTES,
) {
    companion object {
        const val DEFAULT_SNOOZE_MINUTES = 10
        val SNOOZE_OPTIONS = listOf(5, 10, 15, 30)
    }
}

/** Progress numbers shown on the home and success screens. */
data class ProgressStats(
    val today: Int = 0,
    val total: Int = 0,
    val streakDays: Int = 0,
)
