package com.eyecare.domain.usecase

import com.eyecare.core.analytics.AnalyticsEvent
import com.eyecare.core.analytics.AnalyticsTracker
import com.eyecare.core.gamification.Feedback
import com.eyecare.core.gamification.ProgressService
import com.eyecare.core.notification.NotificationManager
import com.eyecare.core.scheduler.ReminderScheduler
import com.eyecare.domain.model.NotificationStatus
import com.eyecare.domain.model.WeeklySchedule
import com.eyecare.domain.repository.ExerciseRepository
import com.eyecare.domain.repository.ReminderRepository
import com.eyecare.domain.repository.ScheduleRepository
import com.eyecare.domain.repository.SettingsRepository
import com.eyecare.domain.time.AppClock

class SaveWeeklyScheduleUseCase(
    private val schedules: ScheduleRepository,
    private val scheduler: ReminderScheduler,
    private val analytics: AnalyticsTracker,
) {
    suspend operator fun invoke(routine: List<WeeklySchedule>) {
        schedules.replaceSchedules(routine)
        analytics.track(AnalyticsEvent.RoutineSaved(activeDays = routine.filter { it.enabled }.map { it.dayOfWeek }.distinct().size))
        scheduler.refresh()
    }
}

class SnoozeReminderUseCase(
    private val reminders: ReminderRepository,
    private val settings: SettingsRepository,
    private val notifier: NotificationManager,
    private val scheduler: ReminderScheduler,
    private val clock: AppClock,
    private val analytics: AnalyticsTracker,
) {
    suspend operator fun invoke(reminderId: Long) {
        val minutes = settings.getSettings().snoozeMinutes
        reminders.snooze(reminderId, clock.now().plusSeconds(minutes * 60L))
        notifier.dismissReminder()
        analytics.track(AnalyticsEvent.ReminderSnoozed(minutes))
        scheduler.refresh()
    }
}

class DismissReminderUseCase(
    private val reminders: ReminderRepository,
    private val notifier: NotificationManager,
    private val analytics: AnalyticsTracker,
) {
    suspend operator fun invoke(reminderId: Long) {
        reminders.setStatus(reminderId, NotificationStatus.SKIPPED)
        notifier.dismissReminder()
        analytics.track(AnalyticsEvent.ReminderDismissed)
    }
}

/** Records a completion. Only called when every step was walked through (section 7). */
class CompleteExerciseUseCase(
    private val exercises: ExerciseRepository,
    private val reminders: ReminderRepository,
    private val progress: ProgressService,
    private val clock: AppClock,
    private val analytics: AnalyticsTracker,
) {
    suspend operator fun invoke(exerciseId: Long, reminderId: Long?): Feedback {
        exercises.recordCompletion(exerciseId, clock.now())
        reminderId?.let { reminders.setStatus(it, NotificationStatus.DONE) }
        analytics.track(AnalyticsEvent.ExerciseCompleted)
        return progress.feedbackFor(progress.currentStats())
    }
}
