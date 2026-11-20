package com.eyecare.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun observe(): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE id = 1")
    suspend fun get(): UserSettingsEntity?

    @Upsert
    suspend fun upsert(settings: UserSettingsEntity)
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM weekly_schedule ORDER BY dayOfWeek, startMinute")
    fun observeAll(): Flow<List<WeeklyScheduleEntity>>

    @Query("SELECT * FROM weekly_schedule ORDER BY dayOfWeek, startMinute")
    suspend fun getAll(): List<WeeklyScheduleEntity>

    @Query("SELECT COUNT(*) FROM weekly_schedule")
    suspend fun count(): Int

    @Query("DELETE FROM weekly_schedule")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(items: List<WeeklyScheduleEntity>)

    @Transaction
    suspend fun replaceAll(items: List<WeeklyScheduleEntity>) {
        deleteAll()
        insertAll(items)
    }
}

@Dao
interface NotificationDao {
    @Insert
    suspend fun insert(item: ScheduledNotificationEntity): Long

    @Query("SELECT MAX(slotAt) FROM scheduled_notification")
    suspend fun maxSlotAt(): Long?

    @Query("SELECT * FROM scheduled_notification WHERE status = 'SNOOZED' ORDER BY nextTriggerAt LIMIT 1")
    suspend fun activeSnooze(): ScheduledNotificationEntity?

    @Query("SELECT * FROM scheduled_notification WHERE status = 'SNOOZED' ORDER BY nextTriggerAt LIMIT 1")
    fun observeActiveSnooze(): Flow<ScheduledNotificationEntity?>

    @Query("UPDATE scheduled_notification SET status = 'SNOOZED', nextTriggerAt = :until WHERE id = :id")
    suspend fun snooze(id: Long, until: Long)

    @Query("UPDATE scheduled_notification SET status = :status WHERE id = :id")
    suspend fun setStatus(id: Long, status: String)

    @Query("UPDATE scheduled_notification SET status = 'SKIPPED' WHERE status = 'SNOOZED'")
    suspend fun skipAllSnoozed()
}

@Dao
interface ExerciseDao {
    @Transaction
    @Query("SELECT * FROM exercise ORDER BY id LIMIT 1")
    suspend fun getFirst(): ExerciseWithSteps?

    @Transaction
    @Query("SELECT * FROM exercise WHERE id = :id")
    suspend fun getById(id: Long): ExerciseWithSteps?

    @Query("SELECT COUNT(*) FROM exercise")
    suspend fun count(): Int

    @Insert
    suspend fun insertExercise(exercise: ExerciseEntity): Long

    @Insert
    suspend fun insertSteps(steps: List<ExerciseStepEntity>)

    @Transaction
    suspend fun insertWithSteps(exercise: ExerciseEntity, steps: List<ExerciseStepEntity>): Long {
        val id = insertExercise(exercise)
        insertSteps(steps.map { it.copy(exerciseId = id) })
        return id
    }

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCompletion(completion: ExerciseCompletionEntity)

    @Query("SELECT * FROM exercise_completion ORDER BY completedAt")
    fun observeCompletions(): Flow<List<ExerciseCompletionEntity>>

    @Query("SELECT * FROM exercise_completion ORDER BY completedAt")
    suspend fun getCompletions(): List<ExerciseCompletionEntity>
}
