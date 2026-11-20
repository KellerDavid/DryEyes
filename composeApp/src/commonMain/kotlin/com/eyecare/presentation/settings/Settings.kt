package com.eyecare.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eyecare.core.advertisement.AdPlacement
import com.eyecare.di.AppContainer
import com.eyecare.domain.model.AppTheme
import com.eyecare.domain.model.UserSettings
import com.eyecare.domain.repository.SettingsRepository
import com.eyecare.presentation.Strings
import com.eyecare.presentation.components.AdBannerView
import com.eyecare.presentation.components.Page
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val scope: CoroutineScope, private val repository: SettingsRepository) {
    val settings: StateFlow<UserSettings> = repository.observeSettings().stateIn(scope, SharingStarted.Eagerly, UserSettings())

    fun setSnooze(minutes: Int) = scope.launch { repository.saveSettings(settings.value.copy(snoozeMinutes = minutes)) }
    fun setTheme(theme: AppTheme) = scope.launch { repository.saveSettings(settings.value.copy(theme = theme)) }
}

@Composable
fun SettingsScreen(container: AppContainer) {
    val scope = rememberCoroutineScope()
    val vm = remember { SettingsViewModel(scope, container.settingsRepository) }
    val settings by vm.settings.collectAsState()

    Page(
        title = Strings.SETTINGS_TITLE,
        onBack = { container.navigator.home() },
        footer = { AdBannerView(container.adManager, AdPlacement.SETTINGS) },
    ) {
        Section(Strings.SNOOZE_LENGTH) {
            UserSettings.SNOOZE_OPTIONS.forEach { m ->
                FilterChip(selected = settings.snoozeMinutes == m, onClick = { vm.setSnooze(m) }, label = { Text(Strings.minutes(m)) })
            }
        }
        Section(Strings.THEME) {
            listOf(AppTheme.SYSTEM to Strings.THEME_SYSTEM, AppTheme.LIGHT to Strings.THEME_LIGHT, AppTheme.DARK to Strings.THEME_DARK)
                .forEach { (theme, label) ->
                    FilterChip(selected = settings.theme == theme, onClick = { vm.setTheme(theme) }, label = { Text(label) })
                }
        }
        Text(Strings.BACKGROUND_NOTE, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Section(title: String, chips: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { chips() }
        }
    }
}
