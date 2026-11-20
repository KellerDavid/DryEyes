package com.eyecare.data.database

import com.eyecare.domain.model.Exercise
import com.eyecare.domain.model.ExerciseCompletion
import com.eyecare.domain.model.ExerciseStep
import com.eyecare.domain.model.NotificationStatus
import com.eyecare.domain.model.ScheduledNotification
import com.eyecare.domain.model.TimeBand
import com.eyecare.domain.model.UserSettings
import java.time.DayOfWeek
import java.time.Instant

fun Set<DayOfWeek>.toMask(): Int = fold(0) { acc, d -> acc or (1 shl (d.value - 1)) }
fun Int.toDays(): Set<DayOfWeek> = DayOfWeek.entries.filter { this and (1 shl (it.value - 1)) != 0 }.toSet()

fun TimeBandEntity.toDomain() = TimeBand(id, daysMask.toDays(), startMinute, endMinute, intervalMinutes, active)
fun TimeBand.toEntity() = TimeBandEntity(id, days.toMask(), startMinute, endMinute, intervalMinutes, active)

fun ScheduledNotificationEntity.toDomain() = ScheduledNotification(
    id = id,
    bandId = bandId,
    slotAt = Instant.ofEpochMilli(slotAt),
    nextTriggerAt = Instant.ofEpochMilli(nextTriggerAt),
    status = NotificationStatus.valueOf(status),
    wasSnoozed = wasSnoozed,
)

fun ExerciseWithSteps.toDomain() = Exercise(
    id = exercise.id,
    name = exercise.name,
    description = exercise.description,
    steps = steps.sortedBy { it.stepOrder }.map {
        ExerciseStep(it.id, it.title, it.instruction, it.durationSeconds, it.stepOrder, it.art)
    },
)

fun ExerciseCompletionEntity.toDomain() = ExerciseCompletion(id, exerciseId, reminderId, Instant.ofEpochMilli(completedAt))

fun UserSettingsEntity.toDomain() = UserSettings(snoozeMinutes, setupAt?.let(Instant::ofEpochMilli))
fun UserSettings.toEntity() = UserSettingsEntity(snoozeMinutes = snoozeMinutes, setupAt = setupAt?.toEpochMilli())
