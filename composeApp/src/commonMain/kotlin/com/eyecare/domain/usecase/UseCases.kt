package com.eyecare.domain.usecase

import com.eyecare.core.analytics.AnalyticsEvent
import com.eyecare.core.analytics.AnalyticsTracker
import com.eyecare.core.gamification.Feedback
import com.eyecare.core.gamification.RoutineProgress
import com.eyecare.core.notification.NotificationManager
import com.eyecare.core.scheduler.NextTriggerCalculator
import com.eyecare.core.scheduler.ReminderScheduler
import com.eyecare.domain.model.NotificationStatus
import com.eyecare.domain.model.TimeBand
import com.eyecare.domain.repository.ExerciseRepository
import com.eyecare.domain.repository.ReminderRepository
import com.eyecare.domain.repository.ScheduleRepository
import com.eyecare.domain.repository.SettingsRepository
import com.eyecare.domain.time.AppClock
import java.time.Duration

class SaveBandUseCase(
    private val schedules: ScheduleRepository,
    private val reminders: ReminderRepository,
    private val settings: SettingsRepository,
    private val scheduler: ReminderScheduler,
    private val clock: AppClock,
    private val analytics: AnalyticsTracker,
) {
    suspend operator fun invoke(band: TimeBand): Long {
        val id = schedules.saveBand(band)
        val current = settings.getSettings()
        if (current.setupAt == null) settings.saveSettings(current.copy(setupAt = clock.now()))
        dropOrphanSnoozes(schedules, reminders, clock)
        analytics.track(AnalyticsEvent.RoutineSaved(activeDays = band.days.size))
        scheduler.refresh()
        return id
    }
}

class DeleteBandUseCase(
    private val schedules: ScheduleRepository,
    private val reminders: ReminderRepository,
    private val scheduler: ReminderScheduler,
    private val clock: AppClock,
) {
    suspend operator fun invoke(id: Long) {
        schedules.deleteBand(id)
        dropOrphanSnoozes(schedules, reminders, clock)
        scheduler.refresh()
    }
}

/** A snoozed reminder whose occurrence no longer exists in the edited routine is dropped. */
private suspend fun dropOrphanSnoozes(schedules: ScheduleRepository, reminders: ReminderRepository, clock: AppClock) {
    val bands = schedules.getBands()
    val now = clock.now()
    val valid = NextTriggerCalculator.slotsBetween(bands, now.minus(Duration.ofDays(2)), now.plus(Duration.ofDays(1)), clock.zone())
        .map { it.bandId to it.at }.toSet()
    reminders.withStatus(NotificationStatus.SNOOZED)
        .filter { (it.bandId to it.slotAt) !in valid }
        .forEach { reminders.delete(it.id) }
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
    private val scheduler: ReminderScheduler,
    private val analytics: AnalyticsTracker,
) {
    suspend operator fun invoke(reminderId: Long) {
        reminders.setStatus(reminderId, NotificationStatus.MISSED)
        notifier.dismissReminder()
        analytics.track(AnalyticsEvent.ReminderDismissed)
        scheduler.refresh()
    }
}

/** Marks the reminder as being answered by an exercise (so a newer reminder doesn't supersede it). */
class StartFromReminderUseCase(
    private val reminders: ReminderRepository,
    private val scheduler: ReminderScheduler,
) {
    suspend operator fun invoke(reminderId: Long) {
        reminders.setStatus(reminderId, NotificationStatus.STARTED)
        scheduler.refresh()
    }
}

class AbortExerciseUseCase(
    private val reminders: ReminderRepository,
    private val scheduler: ReminderScheduler,
) {
    suspend operator fun invoke(reminderId: Long?) {
        reminderId?.let { reminders.setStatus(it, NotificationStatus.ABORTED) }
        scheduler.refresh()
    }
}

/** Records a completion. Only called when every step was walked through (section 7). */
class CompleteExerciseUseCase(
    private val exercises: ExerciseRepository,
    private val reminders: ReminderRepository,
    private val schedules: ScheduleRepository,
    private val scheduler: ReminderScheduler,
    private val clock: AppClock,
    private val analytics: AnalyticsTracker,
) {
    suspend operator fun invoke(exerciseId: Long, reminderId: Long?): Feedback {
        val now = clock.now()
        exercises.recordCompletion(exerciseId, reminderId, now)
        reminderId?.let { reminders.setStatus(it, NotificationStatus.DONE) }
        analytics.track(AnalyticsEvent.ExerciseCompleted)
        scheduler.refresh()
        val since = now.minus(Duration.ofDays(2))
        val instance = NextTriggerCalculator.currentOrNextInstance(schedules.getBands(), now, clock.zone())
        return RoutineProgress.compute(instance, reminders.getSince(since), exercises.getCompletionsSince(since), now, clock.zone())
    }
}

/** Full quit: open reminders can no longer be answered (demo: "Kilépés után nem érkeznek emlékeztetők"). */
class QuitAppUseCase(private val reminders: ReminderRepository) {
    suspend operator fun invoke() {
        reminders.withStatus(NotificationStatus.SHOWN, NotificationStatus.SNOOZED, NotificationStatus.STARTED)
            .forEach { reminders.setStatus(it.id, NotificationStatus.MISSED) }
    }
}
