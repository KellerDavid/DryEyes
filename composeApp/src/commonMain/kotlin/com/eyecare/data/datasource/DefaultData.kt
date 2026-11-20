package com.eyecare.data.datasource

import com.eyecare.domain.model.Exercise
import com.eyecare.domain.model.ExerciseStep
import com.eyecare.domain.model.WeeklySchedule
import com.eyecare.domain.repository.ExerciseRepository
import com.eyecare.domain.repository.ScheduleRepository
import java.time.DayOfWeek
import java.time.LocalTime

/** Built-in content inserted on first launch (section 3.4 and 8.3 of the plan). */
object DefaultData {

    val defaultExercise = Exercise(
        name = "Szemkímélő szünet",
        description = "Rövid, egyperces gyakorlat a szem pihentetésére.",
        steps = listOf(
            ExerciseStep(instruction = "Nézz ki az ablakon vagy egy legalább 6 méterre lévő pontra.", durationSeconds = 20, order = 0),
            ExerciseStep(instruction = "Csukd be a szemed, és lazítsd el az arcizmaidat.", durationSeconds = 10, order = 1),
            ExerciseStep(instruction = "Pislogj tudatosan, lassan tízszer egymás után.", durationSeconds = 10, order = 2),
            ExerciseStep(instruction = "Nézz ismét a távolba, és kövesd a tárgyak körvonalát.", durationSeconds = 20, order = 3),
        ),
    )

    /** Weekdays 09:00–17:00, every 20 minutes (the classic 20-20-20 rule). */
    val defaultRoutine: List<WeeklySchedule> = DayOfWeek.entries.map { day ->
        WeeklySchedule(
            dayOfWeek = day,
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(17, 0),
            intervalMinutes = 20,
            enabled = day.value <= 5,
        )
    }

    suspend fun seedIfEmpty(exercises: ExerciseRepository, schedules: ScheduleRepository) {
        if (exercises.countExercises() == 0) exercises.insertExercise(defaultExercise)
        if (schedules.isEmpty()) schedules.replaceSchedules(defaultRoutine)
    }
}
