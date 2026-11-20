package com.eyecare.core.gamification

import com.eyecare.domain.model.ExerciseCompletion
import com.eyecare.domain.model.ProgressStats
import com.eyecare.domain.repository.ExerciseRepository
import com.eyecare.domain.time.AppClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Positive feedback shown after a completed exercise (US-03, MVP scope). */
data class Feedback(
    val title: String,
    val message: String,
    val stats: ProgressStats,
) {
    val points: Int get() = stats.total * POINTS_PER_EXERCISE

    companion object {
        const val POINTS_PER_EXERCISE = 10
    }
}

class ProgressService(
    private val exercises: ExerciseRepository,
    private val clock: AppClock,
) {
    fun observeStats(): Flow<ProgressStats> =
        exercises.observeCompletions().map { compute(it, clock.now(), clock.zone()) }

    suspend fun currentStats(): ProgressStats = compute(exercises.getCompletions(), clock.now(), clock.zone())

    fun feedbackFor(stats: ProgressStats): Feedback = Feedback(
        title = "Szép munka!",
        message = when {
            stats.total == 1 -> "Megvan az első szemkímélő szüneted. A szemed hálás érte!"
            stats.streakDays >= 3 -> "Már ${stats.streakDays} napja folyamatosan figyelsz a szemedre. Így tovább!"
            stats.today == 1 -> "Megvan a mai első szünet. Jó kezdés!"
            else -> ENCOURAGEMENTS[stats.total % ENCOURAGEMENTS.size]
        },
        stats = stats,
    )

    companion object {
        private val ENCOURAGEMENTS = listOf(
            "A szemed most pihent egy kicsit. Remek!",
            "Egy rövid szünet, nagy különbség. Így tovább!",
            "Ügyes vagy! A rendszeres pihenő a legjobb szemvédelem.",
        )

        fun compute(completions: List<ExerciseCompletion>, now: Instant, zone: ZoneId): ProgressStats {
            val today = now.atZone(zone).toLocalDate()
            val days: Set<LocalDate> = completions.map { it.completedAt.atZone(zone).toLocalDate() }.toSet()
            return ProgressStats(
                today = completions.count { it.completedAt.atZone(zone).toLocalDate() == today },
                total = completions.size,
                streakDays = streak(days, today),
            )
        }

        /** Consecutive days with at least one completion, ending today (or yesterday if today has none yet). */
        private fun streak(days: Set<LocalDate>, today: LocalDate): Int {
            var day = if (today in days) today else today.minusDays(1)
            var count = 0
            while (day in days) {
                count++
                day = day.minusDays(1)
            }
            return count
        }
    }
}
