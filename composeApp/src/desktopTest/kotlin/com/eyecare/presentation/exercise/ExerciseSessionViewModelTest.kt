package com.eyecare.presentation.exercise

import com.eyecare.core.analytics.AnalyticsEvent
import com.eyecare.core.scheduler.ReminderScheduler
import com.eyecare.data.datasource.DefaultData
import com.eyecare.domain.model.NotificationStatus
import com.eyecare.domain.usecase.CompleteExerciseUseCase
import com.eyecare.presentation.exercise.ExerciseSessionViewModel.State
import com.eyecare.testing.FakeClock
import com.eyecare.testing.FakeExerciseRepository
import com.eyecare.testing.FakeReminderRepository
import com.eyecare.testing.FakeScheduleRepository
import com.eyecare.testing.RecordingAnalytics
import com.eyecare.testing.RecordingNotifier
import com.eyecare.testing.band
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

    private val exercise = DefaultData.defaultExercise.copy(id = 7)
    private val clock = FakeClock(local("2026-11-23T10:31"))
    private val exercises = FakeExerciseRepository(exercise)
    private val reminders = FakeReminderRepository()
    private val schedules = FakeScheduleRepository(listOf(band()))
    private val analytics = RecordingAnalytics()
    private val scheduler = ReminderScheduler(schedules, reminders, RecordingNotifier(), clock, analytics)
    private val complete = CompleteExerciseUseCase(exercises, reminders, schedules, scheduler, clock, analytics)

    private fun TestScope.session(reminderId: Long? = null) =
        ExerciseSessionViewModel(this, exercises, complete, analytics, reminderId).also { it.start() }

    @Test
    fun `three steps of 20, 15 and 10 seconds advance automatically`() = runTest {
        val vm = session()
        runCurrent()
        val first = assertIs<State.Running>(vm.state.value)
        assertEquals(0, first.stepIndex)
        assertEquals(20, first.remainingSeconds)
        assertEquals(1f, first.stepFraction)

        advanceTimeBy(20_050)
        assertEquals(1, assertIs<State.Running>(vm.state.value).stepIndex)
        advanceTimeBy(15_000)
        assertEquals(2, assertIs<State.Running>(vm.state.value).stepIndex)
        assertTrue(exercises.completions.value.isEmpty())

        advanceUntilIdle()
        assertIs<State.Finished>(vm.state.value)
        assertEquals(1, exercises.completions.value.size)
        assertTrue(analytics.events.contains(AnalyticsEvent.ExerciseCompleted))
    }

    @Test
    fun `abort never records a completion`() = runTest {
        val vm = session()
        advanceTimeBy(1_500)
        vm.abort()
        advanceUntilIdle()
        assertIs<State.Aborted>(vm.state.value)
        assertTrue(exercises.completions.value.isEmpty())
        assertTrue(analytics.events.contains(AnalyticsEvent.ExerciseCancelled(atStep = 1)))
    }

    @Test
    fun `completion from a reminder marks it done and counts toward today`() = runTest {
        val id = reminders.record(1, local("2026-11-23T10:30"), NotificationStatus.STARTED)
        val vm = session(reminderId = id)
        advanceUntilIdle()
        assertEquals(NotificationStatus.DONE, reminders.status(id))
        val feedback = assertIs<State.Finished>(vm.state.value).feedback
        assertEquals(1, feedback.doneToday)
        assertEquals(6, feedback.total)
        assertEquals("az", feedback.article)
    }

    @Test
    fun `extra exercise outside reminders counts as well`() = runTest {
        reminders.record(1, local("2026-11-23T09:00"), NotificationStatus.DONE)
        val vm = session()
        advanceUntilIdle()
        val feedback = assertIs<State.Finished>(vm.state.value).feedback
        assertEquals(2, feedback.doneToday)
        assertEquals("a", feedback.article)
    }
}
