package com.eyecare.domain.repository

import com.eyecare.domain.model.Exercise
import com.eyecare.domain.model.ExerciseCompletion
import com.eyecare.domain.model.NotificationStatus
import com.eyecare.domain.model.ScheduledNotification
import com.eyecare.domain.model.TimeBand
import com.eyecare.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface ScheduleRepository {
    fun observeBands(): Flow<List<TimeBand>>
    suspend fun getBands(): List<TimeBand>
    /** Inserts (id == 0) or updates a band; returns its id. */
    suspend fun saveBand(band: TimeBand): Long
    suspend fun deleteBand(id: Long)
}

interface ReminderRepository {
    fun observeSince(since: Instant): Flow<List<ScheduledNotification>>
    suspend fun getSince(since: Instant): List<ScheduledNotification>
    suspend fun find(bandId: Long, slotAt: Instant): ScheduledNotification?
    /** Records an occurrence with the given status; returns its id. */
    suspend fun record(bandId: Long, slotAt: Instant, status: NotificationStatus): Long
    suspend fun withStatus(vararg status: NotificationStatus): List<ScheduledNotification>
    suspend fun setStatus(id: Long, status: NotificationStatus)
    suspend fun snooze(id: Long, until: Instant)
    suspend fun delete(id: Long)
}

interface ExerciseRepository {
    suspend fun getDefaultExercise(): Exercise?
    suspend fun insertExercise(exercise: Exercise): Long
    suspend fun countExercises(): Int
    suspend fun recordCompletion(exerciseId: Long, reminderId: Long?, at: Instant)
    fun observeCompletionsSince(since: Instant): Flow<List<ExerciseCompletion>>
    suspend fun getCompletionsSince(since: Instant): List<ExerciseCompletion>
}

interface SettingsRepository {
    fun observeSettings(): Flow<UserSettings>
    suspend fun getSettings(): UserSettings
    suspend fun saveSettings(settings: UserSettings)
}
