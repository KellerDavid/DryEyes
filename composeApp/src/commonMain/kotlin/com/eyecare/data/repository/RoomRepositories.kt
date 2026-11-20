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
import com.eyecare.domain.model.UserSettings
import com.eyecare.domain.model.WeeklySchedule
import com.eyecare.domain.repository.ExerciseRepository
import com.eyecare.domain.repository.ReminderRepository
import com.eyecare.domain.repository.ScheduleRepository
import com.eyecare.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class RoomScheduleRepository(db: AppDatabase) : ScheduleRepository {
    private val dao = db.scheduleDao()
    override fun observeSchedules(): Flow<List<WeeklySchedule>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    override suspend fun getSchedules(): List<WeeklySchedule> = dao.getAll().map { it.toDomain() }
    override suspend fun replaceSchedules(schedules: List<WeeklySchedule>) =
        dao.replaceAll(schedules.map { it.toEntity().copy(id = 0) })
    override suspend fun isEmpty(): Boolean = dao.count() == 0
}

class RoomReminderRepository(db: AppDatabase) : ReminderRepository {
    private val dao = db.notificationDao()

    override suspend fun recordFired(scheduleId: Long?, slotAt: Instant): Long = dao.insert(
        ScheduledNotificationEntity(
            scheduleId = scheduleId,
            slotAt = slotAt.toEpochMilli(),
            nextTriggerAt = slotAt.toEpochMilli(),
            status = NotificationStatus.FIRED.name,
        ),
    )

    override suspend fun lastHandledSlot(): Instant? = dao.maxSlotAt()?.let(Instant::ofEpochMilli)
    override suspend fun activeSnooze(): ScheduledNotification? = dao.activeSnooze()?.toDomain()
    override fun observeActiveSnooze(): Flow<ScheduledNotification?> = dao.observeActiveSnooze().map { it?.toDomain() }
    override suspend fun snooze(id: Long, until: Instant) = dao.snooze(id, until.toEpochMilli())
    override suspend fun setStatus(id: Long, status: NotificationStatus) = dao.setStatus(id, status.name)
    override suspend fun skipAllSnoozed() = dao.skipAllSnoozed()
}

class RoomExerciseRepository(db: AppDatabase) : ExerciseRepository {
    private val dao = db.exerciseDao()

    override suspend fun getDefaultExercise(): Exercise? = dao.getFirst()?.toDomain()
    override suspend fun getExercise(id: Long): Exercise? = dao.getById(id)?.toDomain()
    override suspend fun countExercises(): Int = dao.count()

    override suspend fun insertExercise(exercise: Exercise): Long = dao.insertWithSteps(
        ExerciseEntity(name = exercise.name, description = exercise.description),
        exercise.steps.map {
            ExerciseStepEntity(exerciseId = 0, instruction = it.instruction, durationSeconds = it.durationSeconds, stepOrder = it.order)
        },
    )

    override suspend fun recordCompletion(exerciseId: Long, at: Instant) =
        dao.insertCompletion(ExerciseCompletionEntity(exerciseId = exerciseId, completedAt = at.toEpochMilli()))

    override fun observeCompletions(): Flow<List<ExerciseCompletion>> =
        dao.observeCompletions().map { list -> list.map { it.toDomain() } }

    override suspend fun getCompletions(): List<ExerciseCompletion> = dao.getCompletions().map { it.toDomain() }
}

class RoomSettingsRepository(db: AppDatabase) : SettingsRepository {
    private val dao = db.settingsDao()
    override fun observeSettings(): Flow<UserSettings> = dao.observe().map { it?.toDomain() ?: UserSettings() }
    override suspend fun getSettings(): UserSettings = dao.get()?.toDomain() ?: UserSettings()
    override suspend fun saveSettings(settings: UserSettings) = dao.upsert(settings.toEntity())
}
