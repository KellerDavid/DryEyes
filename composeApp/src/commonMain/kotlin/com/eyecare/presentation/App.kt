package com.eyecare.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.eyecare.di.AppContainer
import com.eyecare.domain.model.UserSettings
import com.eyecare.presentation.exercise.ExerciseScreen
import com.eyecare.presentation.exercise.SuccessScreen
import com.eyecare.presentation.home.HomeScreen
import com.eyecare.presentation.navigation.Screen
import com.eyecare.presentation.schedule.ScheduleScreen
import com.eyecare.presentation.settings.SettingsScreen
import com.eyecare.presentation.theme.EyeCareTheme

@Composable
fun App(container: AppContainer) {
    val settings by container.settingsRepository.observeSettings().collectAsState(UserSettings())
    val screen by container.navigator.screen.collectAsState()

    EyeCareTheme(settings.theme) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            when (val s = screen) {
                Screen.Home -> HomeScreen(container)
                Screen.Schedule -> ScheduleScreen(container)
                Screen.Settings -> SettingsScreen(container)
                is Screen.Exercise -> ExerciseScreen(container, s)
                is Screen.Success -> SuccessScreen(container, s.feedback)
            }
        }
    }
}
