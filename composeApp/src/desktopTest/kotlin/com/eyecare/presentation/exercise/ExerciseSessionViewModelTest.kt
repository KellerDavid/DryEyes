package com.eyecare.presentation.exercise

import com.eyecare.core.analytics.AnalyticsEvent
import com.eyecare.core.gamification.ProgressService
import com.eyecare.domain.model.Exercise
import com.eyecare.domain.model.ExerciseStep
import com.eyecare.domain.model.NotificationStatus
import com.eyecare.domain.usecase.CompleteExerciseUseCase
import com.eyecare.presentation.exercise.ExerciseSessionViewModel.State
import com.eyecare.testing.FakeClock
import com.eyecare.testing.FakeExerciseRepository
import com.eyecare.testing.FakeReminderRepository
import com.eyecare.testing.RecordingAnalytics
import com.eyecare.testing.local
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ExerciseSessionViewModelTest {

    private val exercise = Exercise(
        id = 7,
        name = "Teszt",
        description = "",
        steps = listOf(
            ExerciseStep(instruction = "A", durationSeconds = 3, order = 0),
            ExerciseStep(instruction = "B", durationSeconds = 2, order = 1),
        ),
    )
    private val clock = FakeClock(local("2026-11-23T10:00"))
    private val exercises = FakeExerciseRepository(exercise)
    private val reminders = FakeReminderRepository()
    private val analytics = RecordingAnalytics()
    private val complete = CompleteExerciseUseCase(exercises, reminders, ProgressService(exercises, clock), clock, analytics)

    private fun TestScope.session(reminderId: Long? = null) =
        ExerciseSessionViewModel(this, exercises, complete, analytics, reminderId).also { it.start() }

    @Test
    fun `counts down each step and records completion at the end`() = runTest {
        val vm = session()
        runCurrent()
        val first = assertIs<State.Running>(vm.state.value)
        assertEquals(0, first.stepIndex)
        assertEquals(3, first.remainingSeconds)
        assertEquals(0f, first.progress)

        advanceTimeBy(3_001)
        val second = assertIs<State.Running>(vm.state.value)
        assertEquals(1, second.stepIndex)
        assertEquals(0.6f, second.progress)
        assertTrue(exercises.completions.value.isEmpty())

        advanceUntilIdle()
        val finished = assertIs<State.Finished>(vm.state.value)
        assertEquals(1, finished.feedback.stats.total)
        assertEquals(1, exercises.completions.value.size)
        assertTrue(analytics.events.any { it is AnalyticsEvent.ExerciseStarted })
        assertTrue(analytics.events.contains(AnalyticsEvent.ExerciseCompleted))
    }

    @Test
    fun `cancel never records a completion`() = runTest {
        val vm = session()
        advanceTimeBy(1_500)
        vm.cancel()
        advanceUntilIdle()
        assertIs<State.Cancelled>(vm.state.value)
        assertTrue(exercises.completions.value.isEmpty())
        assertTrue(analytics.events.contains(AnalyticsEvent.ExerciseCancelled(atStep = 1)))
    }

    @Test
    fun `skip moves to the next step immediately`() = runTest {
        val vm = session()
        runCurrent()
        vm.skipStep()
        runCurrent()
        assertEquals(1, assertIs<State.Running>(vm.state.value).stepIndex)
    }

    @Test
    fun `completing from a reminder marks it done`() = runTest {
        val id = reminders.recordFired(1, clock.now())
        session(reminderId = id)
        advanceUntilIdle()
        assertEquals(NotificationStatus.DONE, reminders.status(id))
    }
}
