package com.eyecare.presentation

import com.eyecare.core.gamification.Feedback
import com.eyecare.core.notification.NotificationManager
import com.eyecare.core.notification.ReminderEvent
import com.eyecare.di.AppContainer
import com.eyecare.domain.model.TimeBand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Tab { HOME, SCHEDULE, SETTINGS }

/** Band being edited in the schedule tab (or the first-launch window). */
data class BandDraft(val band: TimeBand, val isNew: Boolean)

/** One exercise run; [run] makes every start distinct so a fresh session is created. */
data class ExerciseRun(val reminderId: Long?, val run: Long)

data class UiState(
    val panelOpen: Boolean = false,
    val tab: Tab = Tab.HOME,
    val draft: BandDraft? = null,
    /** Reminder toast currently shown in the corner. */
    val toast: ReminderEvent? = null,
    /** Reminder that arrived during an exercise/success window; shown when that closes. */
    val queued: ReminderEvent? = null,
    val exercise: ExerciseRun? = null,
    val success: Feedback? = null,
    val onboardingDraft: TimeBand = TimeBand.DEFAULT,
)

/**
 * All window-level UI state (which surfaces are open) and the user actions on them. It is also the
 * NotificationManager: a reminder becomes a corner toast, or is queued while an exercise runs.
 * Persistent effects go through use cases launched in the application scope.
 */
class UiController(private val container: AppContainer, private val appScope: CoroutineScope) : NotificationManager {
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()
    private var runCounter = 0L

    // ----- NotificationManager -----

    override fun showReminder(event: ReminderEvent) = _state.update {
        if (it.exercise != null || it.success != null) it.copy(queued = event, toast = null)
        else it.copy(toast = event, queued = null)
    }

    override fun dismissReminder() = _state.update { it.copy(toast = null) }

    // ----- panel -----

    fun togglePanel() = _state.update { it.copy(panelOpen = !it.panelOpen, tab = Tab.HOME, draft = null) }
    fun openPanel(tab: Tab = Tab.HOME) = _state.update { it.copy(panelOpen = true, tab = tab, draft = null) }
    fun closePanel() = _state.update { it.copy(panelOpen = false) }
    fun selectTab(tab: Tab) = _state.update { it.copy(tab = tab, draft = null) }

    // ----- schedule editing -----

    fun newBand() = _state.update { it.copy(draft = BandDraft(TimeBand.DEFAULT, isNew = true)) }
    fun editBand(band: TimeBand) = _state.update { it.copy(tab = Tab.SCHEDULE, draft = BandDraft(band, isNew = false)) }
    fun cancelEdit() = _state.update { it.copy(draft = null) }
    fun updateDraft(change: (TimeBand) -> TimeBand) = _state.update { s -> s.copy(draft = s.draft?.let { it.copy(band = change(it.band)) }) }

    fun saveDraft() {
        val draft = _state.value.draft ?: return
        if (draft.band.days.isEmpty()) return
        appScope.launch { container.saveBand(draft.band) }
        cancelEdit()
    }

    fun deleteDraft() {
        val draft = _state.value.draft ?: return
        appScope.launch { container.deleteBand(draft.band.id) }
        cancelEdit()
    }

    // ----- first launch -----

    fun updateOnboarding(change: (TimeBand) -> TimeBand) = _state.update { it.copy(onboardingDraft = change(it.onboardingDraft)) }

    fun completeOnboarding() {
        val band = _state.value.onboardingDraft
        if (band.days.isEmpty()) return
        appScope.launch { container.saveBand(band) }
        openPanel(Tab.HOME)
    }

    // ----- reminder toast -----

    fun toastStart() = _state.value.toast?.let { startExercise(it.reminderId) }

    fun toastSnooze() {
        val event = _state.value.toast ?: return
        _state.update { it.copy(toast = null) }
        appScope.launch { container.snoozeReminder(event.reminderId) }
    }

    fun toastClose() {
        val event = _state.value.toast ?: return
        _state.update { it.copy(toast = null) }
        appScope.launch { container.dismissReminder(event.reminderId) }
    }

    // ----- exercise / success -----

    fun startExercise(reminderId: Long? = null) {
        _state.update {
            it.copy(
                panelOpen = false,
                toast = if (reminderId != null && it.toast?.reminderId == reminderId) null else it.toast,
                exercise = ExerciseRun(reminderId, ++runCounter),
                success = null,
            )
        }
        if (reminderId != null) appScope.launch { container.startFromReminder(reminderId) }
    }

    fun exerciseFinished(feedback: Feedback) = _state.update { it.copy(exercise = null, success = feedback) }

    fun exerciseAborted(reminderId: Long?) {
        _state.update { it.copy(exercise = null) }
        appScope.launch { container.abortExercise(reminderId) }
        releaseQueued()
    }

    fun closeSuccess() {
        _state.update { it.copy(success = null) }
        releaseQueued()
    }

    private fun releaseQueued() = _state.update {
        if (it.queued != null && it.exercise == null && it.success == null) it.copy(toast = it.queued, queued = null) else it
    }

    // ----- app -----

    fun quit(exit: () -> Unit) {
        appScope.launch {
            container.quitApp()
            exit()
        }
    }
}
