package com.eyecare.data.database

import com.eyecare.domain.model.AppTheme
import com.eyecare.domain.model.Exercise
import com.eyecare.domain.model.ExerciseCompletion
import com.eyecare.domain.model.ExerciseStep
import com.eyecare.domain.model.NotificationStatus
import com.eyecare.domain.model.ScheduledNotification
import com.eyecare.domain.model.UserSettings
import com.eyecare.domain.model.WeeklySchedule
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime

fun WeeklyScheduleEntity.toDomain() = WeeklySchedule(
    id = id,
    dayOfWeek = DayOfWeek.of(dayOfWeek),
    startTime = LocalTime.ofSecondOfDay(startMinute * 60L),
    endTime = LocalTime.ofSecondOfDay(endMinute * 60L),
    intervalMinutes = intervalMinutes,
    enabled = enabled,
)

fun WeeklySchedule.toEntity() = WeeklyScheduleEntity(
    id = id,
    dayOfWeek = dayOfWeek.value,
    startMinute = startTime.hour * 60 + startTime.minute,
    endMinute = endTime.hour * 60 + endTime.minute,
    intervalMinutes = intervalMinutes,
    enabled = enabled,
)

fun ScheduledNotificationEntity.toDomain() = ScheduledNotification(
    id = id,
    scheduleId = scheduleId,
    slotAt = Instant.ofEpochMilli(slotAt),
    nextTriggerAt = Instant.ofEpochMilli(nextTriggerAt),
    status = NotificationStatus.valueOf(status),
)

fun ExerciseWithSteps.toDomain() = Exercise(
    id = exercise.id,
    name = exercise.name,
    description = exercise.description,
    steps = steps.sortedBy { it.stepOrder }.map {
        ExerciseStep(id = it.id, instruction = it.instruction, durationSeconds = it.durationSeconds, order = it.stepOrder)
    },
)

fun ExerciseCompletionEntity.toDomain() = ExerciseCompletion(
    id = id,
    exerciseId = exerciseId,
    completedAt = Instant.ofEpochMilli(completedAt),
)

fun UserSettingsEntity.toDomain() = UserSettings(
    theme = runCatching { AppTheme.valueOf(theme) }.getOrDefault(AppTheme.SYSTEM),
    snoozeMinutes = snoozeMinutes,
)

fun UserSettings.toEntity() = UserSettingsEntity(theme = theme.name, snoozeMinutes = snoozeMinutes)
