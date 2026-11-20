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
    val theme: String,
    val snoozeMinutes: Int,
) {
    companion object { const val SINGLETON_ID = 1 }
}

/** dayOfWeek: ISO 1 (Monday) … 7 (Sunday); times as minutes of day in local time. */
@Entity(tableName = "weekly_schedule")
data class WeeklyScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayOfWeek: Int,
    val startMinute: Int,
    val endMinute: Int,
    val intervalMinutes: Int,
    val enabled: Boolean,
)

/** Instants as epoch milliseconds (UTC); converted to local time only when calculating/displaying. */
@Entity(
    tableName = "scheduled_notification",
    indices = [Index("status"), Index("slotAt")],
)
data class ScheduledNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scheduleId: Long?,
    val slotAt: Long,
    val nextTriggerAt: Long,
    val status: String,
)

@Entity(tableName = "exercise")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
)

@Entity(
    tableName = "exercise_step",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("exerciseId")],
)
data class ExerciseStepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val instruction: String,
    val durationSeconds: Int,
    val stepOrder: Int,
)

@Entity(
    tableName = "exercise_completion",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("exerciseId"), Index("completedAt")],
)
data class ExerciseCompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val completedAt: Long,
)

data class ExerciseWithSteps(
    @Embedded val exercise: ExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "exerciseId")
    val steps: List<ExerciseStepEntity>,
)
