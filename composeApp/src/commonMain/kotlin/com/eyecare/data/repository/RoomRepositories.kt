package com.eyecare.data.repository

import com.eyecare.data.database.AppDatabase
import com.eyecare.data.database.ExerciseCompletionEntity
import com.eyecare.data.database.ExerciseEntity
import com.eyecare.data.database.ExerciseStepEntity
import com.eyecare.data.database.ScheduledNotificationEntity
import com.eyecare.data.database.toDomain
import com.eyecare.data.database.toEntity
import com.eyecare.domain.model.Exercise
import com.eyecare.domain.model.ExerciseCompletion
import com.eyecare.domain.model.NotificationStatus
import com.eyecare.domain.model.ScheduledNotification
import com.eyecare.domain.model.TimeBand
import com.eyecare.domain.model.UserSettings
import com.eyecare.domain.repository.ExerciseRepository
import com.eyecare.domain.repository.ReminderRepository
import com.eyecare.domain.repository.ScheduleRepository
import com.eyecare.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class RoomScheduleRepository(db: AppDatabase) : ScheduleRepository {
    private val dao = db.timeBandDao()
    override fun observeBands(): Flow<List<TimeBand>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    override suspend fun getBands(): List<TimeBand> = dao.getAll().map { it.toDomain() }
    override suspend fun saveBand(band: TimeBand): Long {
        val rowId = dao.upsert(band.toEntity())
        return if (band.id != 0L) band.id else rowId
    }
    override suspend fun deleteBand(id: Long) = dao.delete(id)
}

class RoomReminderRepository(db: AppDatabase) : ReminderRepository {
    private val dao = db.notificationDao()

    override fun observeSince(since: Instant): Flow<List<ScheduledNotification>> =
        dao.observeSince(since.toEpochMilli()).map { list -> list.map { it.toDomain() } }

    override suspend fun getSince(since: Instant): List<ScheduledNotification> =
        dao.getSince(since.toEpochMilli()).map { it.toDomain() }

    override suspend fun find(bandId: Long, slotAt: Instant): ScheduledNotification? =
        dao.find(bandId, slotAt.toEpochMilli())?.toDomain()

    override suspend fun record(bandId: Long, slotAt: Instant, status: NotificationStatus): Long = dao.insert(
        ScheduledNotificationEntity(
            bandId = bandId,
            slotAt = slotAt.toEpochMilli(),
            nextTriggerAt = slotAt.toEpochMilli(),
            status = status.name,
            wasSnoozed = false,
        ),
    )

    override suspend fun withStatus(vararg status: NotificationStatus): List<ScheduledNotification> =
        dao.withStatus(status.map { it.name }).map { it.toDomain() }

    override suspend fun setStatus(id: Long, status: NotificationStatus) = dao.setStatus(id, status.name)
    override suspend fun snooze(id: Long, until: Instant) = dao.snooze(id, until.toEpochMilli())
    override suspend fun delete(id: Long) = dao.delete(id)
}

class RoomExerciseRepository(db: AppDatabase) : ExerciseRepository {
    private val dao = db.exerciseDao()

    override suspend fun getDefaultExercise(): Exercise? = dao.getFirst()?.toDomain()
    override suspend fun countExercises(): Int = dao.count()

    override suspend fun insertExercise(exercise: Exercise): Long = dao.insertWithSteps(
        ExerciseEntity(name = exercise.name, description = exercise.description),
        exercise.steps.map {
            ExerciseStepEntity(
                exerciseId = 0,
                title = it.title,
                instruction = it.instruction,
                durationSeconds = it.durationSeconds,
                stepOrder = it.order,
                art = it.art,
            )
        },
    )

    override suspend fun recordCompletion(exerciseId: Long, reminderId: Long?, at: Instant) =
        dao.insertCompletion(ExerciseCompletionEntity(exerciseId = exerciseId, reminderId = reminderId, completedAt = at.toEpochMilli()))

    override fun observeCompletionsSince(since: Instant): Flow<List<ExerciseCompletion>> =
        dao.observeCompletionsSince(since.toEpochMilli()).map { list -> list.map { it.toDomain() } }

    override suspend fun getCompletionsSince(since: Instant): List<ExerciseCompletion> =
        dao.getCompletionsSince(since.toEpochMilli()).map { it.toDomain() }
}

class RoomSettingsRepository(db: AppDatabase) : SettingsRepository {
    private val dao = db.settingsDao()
    override fun observeSettings(): Flow<UserSettings> = dao.observe().map { it?.toDomain() ?: UserSettings() }
    override suspend fun getSettings(): UserSettings = dao.get()?.toDomain() ?: UserSettings()
    override suspend fun saveSettings(settings: UserSettings) = dao.upsert(settings.toEntity())
}
