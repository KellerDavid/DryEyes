package com.eyecare.data.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val snoozeMinutes: Int,
    val setupAt: Long?,
) {
    companion object { const val SINGLETON_ID = 1 }
}

/** daysMask: bit (dayOfWeek - 1), Monday = bit 0; minutes are local minutes of day. */
@Entity(tableName = "time_band")
data class TimeBandEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val daysMask: Int,
    val startMinute: Int,
    val endMinute: Int,
    val intervalMinutes: Int,
    val active: Boolean,
)

/** Instants as epoch milliseconds (UTC). One row per routine occurrence that was handled. */
@Entity(
    tableName = "scheduled_notification",
    indices = [Index(value = ["bandId", "slotAt"], unique = true), Index("status"), Index("slotAt")],
)
data class ScheduledNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bandId: Long,
    val slotAt: Long,
    val nextTriggerAt: Long,
    val status: String,
    val wasSnoozed: Boolean,
)

@Entity(tableName = "exercise")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
)

@Entity(
    tableName = "exercise_step",
    foreignKeys = [ForeignKey(ExerciseEntity::class, ["id"], ["exerciseId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("exerciseId")],
)
data class ExerciseStepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val title: String,
    val instruction: String,
    val durationSeconds: Int,
    val stepOrder: Int,
    val art: String,
)

@Entity(
    tableName = "exercise_completion",
    foreignKeys = [ForeignKey(ExerciseEntity::class, ["id"], ["exerciseId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("exerciseId"), Index("completedAt")],
)
data class ExerciseCompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val reminderId: Long?,
    val completedAt: Long,
)

data class ExerciseWithSteps(
    @Embedded val exercise: ExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "exerciseId")
    val steps: List<ExerciseStepEntity>,
)
