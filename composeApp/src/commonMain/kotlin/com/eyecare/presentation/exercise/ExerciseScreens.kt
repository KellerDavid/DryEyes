package com.eyecare.presentation.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eyecare.core.gamification.Feedback
import com.eyecare.di.AppContainer
import com.eyecare.presentation.Strings
import com.eyecare.presentation.home.Stat
import com.eyecare.presentation.navigation.Screen
import com.eyecare.presentation.exercise.ExerciseSessionViewModel.State

/** No ads here: the exercise must never be obstructed (US-18). */
@Composable
fun ExerciseScreen(container: AppContainer, screen: Screen.Exercise) {
    val scope = rememberCoroutineScope() // cancelled when the screen leaves → countdown stops
    val vm = remember(screen) { container.exerciseSession(scope, screen.reminderId).also { it.start() } }
    val state by vm.state.collectAsState()

    LaunchedEffect(state) {
        when (val s = state) {
            is State.Finished -> container.navigator.go(Screen.Success(s.feedback))
            is State.Cancelled -> container.navigator.home()
            else -> Unit
        }
    }

    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        when (val s = state) {
            is State.Running -> RunningStep(s, onSkip = vm::skipStep, onCancel = vm::cancel)
            is State.Unavailable -> {
                Text(Strings.EXERCISE_UNAVAILABLE)
                TextButton(onClick = { container.navigator.home() }) { Text(Strings.BACK) }
            }
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
    }
}

@Composable
private fun ColumnScope.RunningStep(s: State.Running, onSkip: () -> Unit, onCancel: () -> Unit) {
    Text(s.exercise.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    Text("${Strings.STEP} ${s.stepIndex + 1} / ${s.stepCount}", style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(12.dp))
    LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.weight(1f))
    Text(
        s.step.instruction,
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(24.dp))
    Text(
        "${s.remainingSeconds}",
        fontSize = 88.sp,
        fontWeight = FontWeight.Light,
        color = MaterialTheme.colorScheme.primary,
    )
    Text(Strings.SECONDS, style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.weight(1f))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text(Strings.CANCEL) }
        Button(onClick = onSkip, modifier = Modifier.weight(1f)) { Text(Strings.SKIP) }
    }
}

/** Positive feedback after completion (US-03). */
@Composable
fun SuccessScreen(container: AppContainer, feedback: Feedback) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🎉", fontSize = 64.sp)
        Spacer(Modifier.height(8.dp))
        Text(feedback.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(feedback.message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Card(Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Stat(Strings.SUCCESS_TODAY, feedback.stats.today.toString())
                Stat(Strings.SUCCESS_TOTAL, feedback.stats.total.toString())
                Stat(Strings.SUCCESS_STREAK, Strings.days(feedback.stats.streakDays))
                Stat(Strings.SUCCESS_POINTS, feedback.points.toString())
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = { container.navigator.home() }) { Text(Strings.BACK_HOME) }
    }
}
