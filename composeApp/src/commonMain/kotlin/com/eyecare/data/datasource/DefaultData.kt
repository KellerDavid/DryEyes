package com.eyecare.data.datasource

import com.eyecare.domain.model.Exercise
import com.eyecare.domain.model.ExerciseStep
import com.eyecare.domain.repository.ExerciseRepository

/**
 * Built-in exercise pack inserted on first launch. The weekly routine is not seeded:
 * the user sets it up in the first-launch window.
 */
object DefaultData {

    val defaultExercise = Exercise(
        name = "Szemtorna",
        description = "45 mp-es szemtorna, 3 egyszerű lépés.",
        steps = listOf(
            ExerciseStep(
                title = "Nézz távolra",
                instruction = "Keress egy legalább 6 méterre lévő pontot, például az ablakon kívül, és nézd nyugodtan.",
                durationSeconds = 20, order = 0, art = "far",
            ),
            ExerciseStep(
                title = "Csukd be a szemed",
                instruction = "Lazítsd el a szemhéjad, és vegyél egy mély levegőt. Jelzünk, ha letelt az idő.",
                durationSeconds = 15, order = 1, art = "closed",
            ),
            ExerciseStep(
                title = "Pislogj tudatosan",
                instruction = "Pislogj lassan, és minden pislogásnál zárd össze teljesen a szemhéjad.",
                durationSeconds = 10, order = 2, art = "blink",
            ),
        ),
    )

    suspend fun seedIfEmpty(exercises: ExerciseRepository) {
        if (exercises.countExercises() == 0) exercises.insertExercise(defaultExercise)
    }
}
