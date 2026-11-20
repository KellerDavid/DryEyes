package com.eyecare.presentation.exercise

import com.eyecare.core.analytics.AnalyticsEvent
import com.eyecare.core.analytics.AnalyticsTracker
import com.eyecare.core.gamification.Feedback
import com.eyecare.domain.model.Exercise
import com.eyecare.domain.repository.ExerciseRepository
import com.eyecare.domain.usecase.CompleteExerciseUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Step-by-step exercise runner (section 3.4): shows the current instruction, counts down its
 * duration and advances. The scope is owned by the exercise screen, so leaving the screen stops it.
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
        data class Running(val exercise: Exercise, val stepIndex: Int, val remainingSeconds: Int) : State {
            val step get() = exercise.steps[stepIndex]
            val stepCount get() = exercise.steps.size
            /** Overall progress 0..1 across all steps. */
            val progress: Float
                get() {
                    val done = exercise.steps.take(stepIndex).sumOf { it.durationSeconds } + (step.durationSeconds - remainingSeconds)
                    return if (exercise.totalSeconds == 0) 1f else done.toFloat() / exercise.totalSeconds
                }
        }
        data class Finished(val feedback: Feedback) : State
        data object Cancelled : State
        data object Unavailable : State
    }

    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()

    private val skipRequests = Channel<Unit>(Channel.CONFLATED)
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
                var remaining = exercise.steps[index].durationSeconds
                _state.value = State.Running(exercise, index, remaining)
                while (remaining > 0) {
                    val skipped = withTimeoutOrNull(1_000) { skipRequests.receive() } != null
                    if (skipped) break
                    remaining--
                    _state.value = State.Running(exercise, index, remaining)
                }
            }
            val feedback = completeExercise(exercise.id, reminderId)
            _state.value = State.Finished(feedback)
        }
    }

    /** Jump to the next step (on the last step this finishes the exercise). */
    fun skipStep() {
        if (_state.value is State.Running) skipRequests.trySend(Unit)
    }

    /** Stop without recording a completion. */
    fun cancel() {
        val current = _state.value
        if (current is State.Finished || current is State.Cancelled) return
        job?.cancel()
        if (current is State.Running) analytics.track(AnalyticsEvent.ExerciseCancelled(atStep = current.stepIndex + 1))
        _state.value = State.Cancelled
    }
}
