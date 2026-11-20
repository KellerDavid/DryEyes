package com.eyecare.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eyecare.core.advertisement.AdPlacement
import com.eyecare.core.gamification.ProgressService
import com.eyecare.core.scheduler.ReminderScheduler
import com.eyecare.di.AppContainer
import com.eyecare.domain.model.ProgressStats
import com.eyecare.domain.repository.ReminderRepository
import com.eyecare.domain.time.AppClock
import com.eyecare.presentation.Strings
import com.eyecare.presentation.components.AdBannerView
import com.eyecare.presentation.components.Page
import com.eyecare.presentation.navigation.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

data class HomeState(
    val nextReminderText: String = "",
    val hasActiveRoutine: Boolean = false,
    val isSnoozed: Boolean = false,
    val stats: ProgressStats = ProgressStats(),
)

class HomeViewModel(
    scope: CoroutineScope,
    scheduler: ReminderScheduler,
    reminders: ReminderRepository,
    progress: ProgressService,
    clock: AppClock,
) {
    private val ticker = flow {
        while (true) {
            emit(clock.now())
            delay(30_000)
        }
    }

    val state: StateFlow<HomeState> = combine(
        scheduler.nextTrigger,
        reminders.observeActiveSnooze(),
        progress.observeStats(),
        ticker,
    ) { next, snooze, stats, now ->
        HomeState(
            nextReminderText = next?.let { Strings.formatUpcoming(it, now, clock.zone()) } ?: Strings.NO_ACTIVE_ROUTINE,
            hasActiveRoutine = next != null,
            isSnoozed = snooze != null && snooze.nextTriggerAt == next,
            stats = stats,
        )
    }.stateIn(scope, SharingStarted.Eagerly, HomeState())
}

@Composable
fun HomeScreen(container: AppContainer) {
    val scope = rememberCoroutineScope()
    val vm = remember {
        HomeViewModel(scope, container.scheduler, container.reminderRepository, container.progressService, container.clock)
    }
    val state by vm.state.collectAsState()
    val nav = container.navigator

    Page(
        title = Strings.HOME_TITLE,
        footer = { AdBannerView(container.adManager, AdPlacement.HOME) },
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(Strings.NEXT_REMINDER, style = MaterialTheme.typography.labelLarge)
                Text(
                    state.nextReminderText + if (state.isSnoozed) " ${Strings.SNOOZED_SUFFIX}" else "",
                    style = if (state.hasActiveRoutine) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(Strings.TODAY_PROGRESS, style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Stat(Strings.SUCCESS_TODAY, state.stats.today.toString())
                    Stat(Strings.SUCCESS_TOTAL, state.stats.total.toString())
                    Stat(Strings.SUCCESS_STREAK, Strings.days(state.stats.streakDays))
                }
            }
        }

        Button(onClick = { nav.startExercise() }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text(Strings.START_NOW, style = MaterialTheme.typography.titleMedium)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { nav.go(Screen.Schedule) }, modifier = Modifier.weight(1f)) { Text(Strings.EDIT_ROUTINE) }
            OutlinedButton(onClick = { nav.go(Screen.Settings) }, modifier = Modifier.weight(1f)) { Text(Strings.SETTINGS) }
        }
    }
}

@Composable
fun Stat(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
