package com.eyecare.presentation.navigation

import com.eyecare.core.gamification.Feedback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface Screen {
    data object Home : Screen
    data object Schedule : Screen
    data object Settings : Screen
    /** [run] makes every start a distinct screen so a fresh session is created. */
    data class Exercise(val reminderId: Long?, val run: Long) : Screen
    data class Success(val feedback: Feedback) : Screen
}

/** Tiny single-stack navigator; hoisted so the tray menu and the reminder popup can navigate too. */
class Navigator {
    private val _screen = MutableStateFlow<Screen>(Screen.Home)
    val screen: StateFlow<Screen> = _screen.asStateFlow()
    private var runCounter = 0L

    fun go(screen: Screen) {
        _screen.value = screen
    }

    fun home() = go(Screen.Home)

    fun startExercise(reminderId: Long? = null) = go(Screen.Exercise(reminderId, ++runCounter))
}
