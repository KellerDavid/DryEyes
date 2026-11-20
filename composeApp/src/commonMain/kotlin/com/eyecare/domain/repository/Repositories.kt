package com.eyecare.domain.repository

import com.eyecare.domain.model.Exercise
import com.eyecare.domain.model.ExerciseCompletion
import com.eyecare.domain.model.NotificationStatus
import com.eyecare.domain.model.ScheduledNotification
import com.eyecare.domain.model.UserSettings
import com.eyecare.domain.model.WeeklySchedule
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface ScheduleRepository {
    fun observeSchedules(): Flow<List<WeeklySchedule>>
    suspend fun getSchedules(): List<WeeklySchedule>
    /** Replaces the whole weekly routine. */
    suspend fun replaceSchedules(schedules: List<WeeklySchedule>)
    suspend fun isEmpty(): Boolean
}

interface ReminderRepository {
    /** Inserts a reminder that has just been shown; returns its id. */
    suspend fun recordFired(scheduleId: Long?, slotAt: Instant): Long
    /** Latest routine slot that has already been handled (shown, snoozed, done or skipped). */
    suspend fun lastHandledSlot(): Instant?
    suspend fun activeSnooze(): ScheduledNotification?
    fun observeActiveSnooze(): Flow<ScheduledNotification?>
    suspend fun snooze(id: Long, until: Instant)
    suspend fun setStatus(id: Long, status: NotificationStatus)
    /** Marks every still-snoozed reminder as skipped (a newer reminder supersedes them). */
    suspend fun skipAllSnoozed()
}

interface ExerciseRepository {
    suspend fun getDefaultExercise(): Exercise?
    suspend fun getExercise(id: Long): Exercise?
    suspend fun insertExercise(exercise: Exercise): Long
    suspend fun countExercises(): Int
    suspend fun recordCompletion(exerciseId: Long, at: Instant)
    fun observeCompletions(): Flow<List<ExerciseCompletion>>
    suspend fun getCompletions(): List<ExerciseCompletion>
}

interface SettingsRepository {
    fun observeSettings(): Flow<UserSettings>
    suspend fun getSettings(): UserSettings
    suspend fun saveSettings(settings: UserSettings)
}
