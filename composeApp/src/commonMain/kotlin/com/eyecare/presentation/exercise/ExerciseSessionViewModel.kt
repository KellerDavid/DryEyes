package com.eyecare.presentation.exercise

import com.eyecare.core.analytics.AnalyticsEvent
import com.eyecare.core.analytics.AnalyticsTracker
import com.eyecare.core.gamification.Feedback
import com.eyecare.domain.model.Exercise
import com.eyecare.domain.repository.ExerciseRepository
import com.eyecare.domain.usecase.CompleteExerciseUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Step-by-step exercise runner (section 3.4). Steps advance automatically in real time; the only
 * user action is abort. The scope is owned by the exercise window, so closing it stops the timer.
 */
class ExerciseSessionViewModel(
    private val scope: CoroutineScope,
    private val exercises: ExerciseRepository,
    private val completeExercise: CompleteExerciseUseCase,
    private val analytics: AnalyticsTracker,
    private val reminderId: Long?,
) {
    sealed interface State {
        data object Loading : State
        data class Running(val exercise: Exercise, val stepIndex: Int, val remainingMillis: Long) : State {
            val step get() = exercise.steps[stepIndex]
            val stepCount get() = exercise.steps.size
            val remainingSeconds: Int get() = ((remainingMillis + 999) / 1000).toInt()
            /** Ring fill for the current step, 1 → 0. */
            val stepFraction: Float get() = remainingMillis.toFloat() / (step.durationSeconds * 1000L)
        }
        data class Finished(val feedback: Feedback) : State
        data object Aborted : State
        data object Unavailable : State
    }

    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()
    private var job: Job? = null

    fun start() {
        if (job != null) return
        job = scope.launch {
            val exercise = exercises.getDefaultExercise()
            if (exercise == null || exercise.steps.isEmpty()) {
                _state.value = State.Unavailable
                return@launch
            }
            analytics.track(AnalyticsEvent.ExerciseStarted(fromReminder = reminderId != null))
            for (index in exercise.steps.indices) {
                var remaining = exercise.steps[index].durationSeconds * 1000L
                while (remaining > 0) {
                    _state.value = State.Running(exercise, index, remaining)
                    delay(TICK_MS)
                    remaining -= TICK_MS
                }
            }
            _state.value = State.Finished(completeExercise(exercise.id, reminderId))
        }
    }

    /** Stop without recording a completion; the caller marks the reminder aborted. */
    fun abort() {
        val current = _state.value
        if (current is State.Finished || current is State.Aborted) return
        job?.cancel()
        if (current is State.Running) analytics.track(AnalyticsEvent.ExerciseCancelled(atStep = current.stepIndex + 1))
        _state.value = State.Aborted // the owner records the abort (it outlives this scope)
    }

    companion object {
        const val TICK_MS = 100L
    }
}
