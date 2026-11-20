package com.eyecare.data.database

import androidx.room.Dao
import androidx.room.Insert
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
interface TimeBandDao {
    @Query("SELECT * FROM time_band ORDER BY id")
    fun observeAll(): Flow<List<TimeBandEntity>>

    @Query("SELECT * FROM time_band ORDER BY id")
    suspend fun getAll(): List<TimeBandEntity>

    @Upsert
    suspend fun upsert(band: TimeBandEntity): Long

    @Query("DELETE FROM time_band WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM scheduled_notification WHERE slotAt >= :since OR status = 'SNOOZED' ORDER BY slotAt")
    fun observeSince(since: Long): Flow<List<ScheduledNotificationEntity>>

    @Query("SELECT * FROM scheduled_notification WHERE slotAt >= :since OR status = 'SNOOZED' ORDER BY slotAt")
    suspend fun getSince(since: Long): List<ScheduledNotificationEntity>

    @Query("SELECT * FROM scheduled_notification WHERE bandId = :bandId AND slotAt = :slotAt")
    suspend fun find(bandId: Long, slotAt: Long): ScheduledNotificationEntity?

    @Insert
    suspend fun insert(item: ScheduledNotificationEntity): Long

    @Query("SELECT * FROM scheduled_notification WHERE status IN (:statuses) ORDER BY nextTriggerAt")
    suspend fun withStatus(statuses: List<String>): List<ScheduledNotificationEntity>

    @Query("UPDATE scheduled_notification SET status = :status WHERE id = :id")
    suspend fun setStatus(id: Long, status: String)

    @Query("UPDATE scheduled_notification SET status = 'SNOOZED', wasSnoozed = 1, nextTriggerAt = :until WHERE id = :id")
    suspend fun snooze(id: Long, until: Long)

    @Query("DELETE FROM scheduled_notification WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface ExerciseDao {
    @Transaction
    @Query("SELECT * FROM exercise ORDER BY id LIMIT 1")
    suspend fun getFirst(): ExerciseWithSteps?

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

    @Insert
    suspend fun insertCompletion(completion: ExerciseCompletionEntity)

    @Query("SELECT * FROM exercise_completion WHERE completedAt >= :since ORDER BY completedAt")
    fun observeCompletionsSince(since: Long): Flow<List<ExerciseCompletionEntity>>

    @Query("SELECT * FROM exercise_completion WHERE completedAt >= :since ORDER BY completedAt")
    suspend fun getCompletionsSince(since: Long): List<ExerciseCompletionEntity>
}
