package com.eyecare.core.gamification

import com.eyecare.core.scheduler.BandInstance
import com.eyecare.domain.model.ExerciseCompletion
import com.eyecare.domain.model.NotificationStatus
import com.eyecare.domain.model.ScheduledNotification
import java.time.Instant
import java.time.ZoneId

/** Positive feedback after a completed exercise (US-03, MVP scope): "this was your Nth break today". */
data class Feedback(val doneToday: Int, val total: Int) {
    /** Hungarian article before the ordinal: "az 1.", "az 5.", "a 2." */
    val article: String get() = if (doneToday == 1 || doneToday.toString().startsWith("5")) "az" else "a"
    val fraction: Float get() = if (total == 0) 1f else (doneToday.toFloat() / total).coerceAtMost(1f)
}

object RoutineProgress {

    /**
     * Completed breaks in the band instance: reminders answered with an exercise plus exercises
     * started outside the routine during the band. Without an instance (no active routine), today's
     * completions are counted against a total of 0.
     */
    fun compute(
        instance: BandInstance?,
        rows: List<ScheduledNotification>,
        completions: List<ExerciseCompletion>,
        now: Instant,
        zone: ZoneId,
    ): Feedback {
        if (instance == null) {
            val today = now.atZone(zone).toLocalDate()
            return Feedback(completions.count { it.completedAt.atZone(zone).toLocalDate() == today }, 0)
        }
        val slotKeys = instance.slots.map { it.bandId to it.at }.toSet()
        val doneSlots = rows.count { it.status == NotificationStatus.DONE && (it.bandId to it.slotAt) in slotKeys }
        val extras = completions.count { it.reminderId == null && it.completedAt in instance }
        return Feedback(doneSlots + extras, instance.slots.size)
    }
}
