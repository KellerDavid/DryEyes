package com.eyecare.core.analytics

/** Product events from section 3.8 of the architecture plan. */
sealed class AnalyticsEvent(val name: String, val properties: Map<String, Any> = emptyMap()) {
    data class RoutineSaved(val activeDays: Int) : AnalyticsEvent("routine_saved", mapOf("active_days" to activeDays))
    data object ReminderShown : AnalyticsEvent("reminder_shown")
    data class ReminderSnoozed(val minutes: Int) : AnalyticsEvent("reminder_snoozed", mapOf("minutes" to minutes))
    data object ReminderDismissed : AnalyticsEvent("reminder_dismissed")
    data class ExerciseStarted(val fromReminder: Boolean) : AnalyticsEvent("exercise_started", mapOf("from_reminder" to fromReminder))
    data object ExerciseCompleted : AnalyticsEvent("exercise_completed")
    data class ExerciseCancelled(val atStep: Int) : AnalyticsEvent("exercise_cancelled", mapOf("at_step" to atStep))
}

/**
 * Analytics must never affect core behaviour: implementations swallow their own errors
 * and work offline. A PostHog-backed implementation can be dropped in later.
 */
interface AnalyticsTracker {
    fun track(event: AnalyticsEvent)
}

/** MVP default: nothing leaves the device. */
object NoOpAnalyticsTracker : AnalyticsTracker {
    override fun track(event: AnalyticsEvent) = Unit
}
