package com.eyecare.di

import com.eyecare.core.advertisement.AdManager
import com.eyecare.core.analytics.AnalyticsTracker
import com.eyecare.core.analytics.NoOpAnalyticsTracker
import com.eyecare.core.gamification.ProgressService
import com.eyecare.core.notification.NotificationManager
import com.eyecare.core.scheduler.ReminderScheduler
import com.eyecare.data.database.AppDatabase
import com.eyecare.data.datasource.DefaultData
import com.eyecare.data.repository.RoomExerciseRepository
import com.eyecare.data.repository.RoomReminderRepository
import com.eyecare.data.repository.RoomScheduleRepository
import com.eyecare.data.repository.RoomSettingsRepository
import com.eyecare.domain.time.AppClock
import com.eyecare.domain.time.SystemAppClock
import com.eyecare.domain.usecase.CompleteExerciseUseCase
import com.eyecare.domain.usecase.DismissReminderUseCase
import com.eyecare.domain.usecase.SaveWeeklyScheduleUseCase
import com.eyecare.domain.usecase.SnoozeReminderUseCase
import com.eyecare.presentation.exercise.ExerciseSessionViewModel
import com.eyecare.presentation.navigation.Navigator
import kotlinx.coroutines.CoroutineScope

/** Manual dependency wiring (no DI framework needed at MVP size). */
class AppContainer(
    database: AppDatabase,
    val notifier: NotificationManager,
    val clock: AppClock = SystemAppClock,
    val analytics: AnalyticsTracker = NoOpAnalyticsTracker,
) {
    val scheduleRepository = RoomScheduleRepository(database)
    val reminderRepository = RoomReminderRepository(database)
    val exerciseRepository = RoomExerciseRepository(database)
    val settingsRepository = RoomSettingsRepository(database)

    val scheduler = ReminderScheduler(scheduleRepository, reminderRepository, notifier, clock, analytics)
    val progressService = ProgressService(exerciseRepository, clock)
    val adManager = AdManager()
    val navigator = Navigator()

    val saveWeeklySchedule = SaveWeeklyScheduleUseCase(scheduleRepository, scheduler, analytics)
    val snoozeReminder = SnoozeReminderUseCase(reminderRepository, settingsRepository, notifier, scheduler, clock, analytics)
    val dismissReminder = DismissReminderUseCase(reminderRepository, notifier, analytics)
    val completeExercise = CompleteExerciseUseCase(exerciseRepository, reminderRepository, progressService, clock, analytics)

    fun exerciseSession(scope: CoroutineScope, reminderId: Long?) =
        ExerciseSessionViewModel(scope, exerciseRepository, completeExercise, analytics, reminderId)

    /** First-launch data (default exercise pack and weekly routine). */
    suspend fun initialize() = DefaultData.seedIfEmpty(exerciseRepository, scheduleRepository)
}
