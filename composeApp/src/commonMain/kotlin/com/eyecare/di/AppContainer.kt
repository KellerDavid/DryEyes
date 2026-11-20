package com.eyecare.di

import com.eyecare.core.advertisement.AdManager
import com.eyecare.core.analytics.AnalyticsTracker
import com.eyecare.core.analytics.NoOpAnalyticsTracker
import com.eyecare.core.scheduler.ReminderScheduler
import com.eyecare.data.database.AppDatabase
import com.eyecare.data.datasource.DefaultData
import com.eyecare.data.repository.RoomExerciseRepository
import com.eyecare.data.repository.RoomReminderRepository
import com.eyecare.data.repository.RoomScheduleRepository
import com.eyecare.data.repository.RoomSettingsRepository
import com.eyecare.domain.time.AppClock
import com.eyecare.domain.time.SystemAppClock
import com.eyecare.domain.usecase.AbortExerciseUseCase
import com.eyecare.domain.usecase.CompleteExerciseUseCase
import com.eyecare.domain.usecase.DeleteBandUseCase
import com.eyecare.domain.usecase.DismissReminderUseCase
import com.eyecare.domain.usecase.QuitAppUseCase
import com.eyecare.domain.usecase.SaveBandUseCase
import com.eyecare.domain.usecase.SnoozeReminderUseCase
import com.eyecare.domain.usecase.StartFromReminderUseCase
import com.eyecare.presentation.UiController
import com.eyecare.presentation.exercise.ExerciseSessionViewModel
import kotlinx.coroutines.CoroutineScope

/** Manual dependency wiring (no DI framework needed at MVP size). */
class AppContainer(
    database: AppDatabase,
    appScope: CoroutineScope,
    val clock: AppClock = SystemAppClock,
    val analytics: AnalyticsTracker = NoOpAnalyticsTracker,
) {
    val scheduleRepository = RoomScheduleRepository(database)
    val reminderRepository = RoomReminderRepository(database)
    val exerciseRepository = RoomExerciseRepository(database)
    val settingsRepository = RoomSettingsRepository(database)

    /** UI state holder; also the NotificationManager the scheduler talks to. */
    val ui = UiController(this, appScope)
    val scheduler = ReminderScheduler(scheduleRepository, reminderRepository, ui, clock, analytics)
    val adManager = AdManager()

    val saveBand = SaveBandUseCase(scheduleRepository, reminderRepository, settingsRepository, scheduler, clock, analytics)
    val deleteBand = DeleteBandUseCase(scheduleRepository, reminderRepository, scheduler, clock)
    val snoozeReminder = SnoozeReminderUseCase(reminderRepository, settingsRepository, ui, scheduler, clock, analytics)
    val dismissReminder = DismissReminderUseCase(reminderRepository, ui, scheduler, analytics)
    val startFromReminder = StartFromReminderUseCase(reminderRepository, scheduler)
    val abortExercise = AbortExerciseUseCase(reminderRepository, scheduler)
    val completeExercise = CompleteExerciseUseCase(exerciseRepository, reminderRepository, scheduleRepository, scheduler, clock, analytics)
    val quitApp = QuitAppUseCase(reminderRepository)

    fun exerciseSession(scope: CoroutineScope, reminderId: Long?) =
        ExerciseSessionViewModel(scope, exerciseRepository, completeExercise, analytics, reminderId)

    /** First-launch data and recovery of reminders left open by a previous run. */
    suspend fun initialize() {
        DefaultData.seedIfEmpty(exerciseRepository)
        scheduler.recoverAfterRestart()
    }
}
