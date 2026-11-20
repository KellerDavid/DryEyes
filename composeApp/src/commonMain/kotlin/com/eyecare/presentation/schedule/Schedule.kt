package com.eyecare.presentation.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.eyecare.domain.model.WeeklySchedule
import com.eyecare.domain.repository.ScheduleRepository
import com.eyecare.domain.usecase.SaveWeeklyScheduleUseCase
import com.eyecare.di.AppContainer
import com.eyecare.presentation.Strings
import com.eyecare.presentation.components.Page
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/** Editable state of one day row; text fields stay raw until saved. */
data class DayRow(
    val day: DayOfWeek,
    val enabled: Boolean,
    val start: String,
    val end: String,
    val interval: String,
    val error: String? = null,
)

data class ScheduleState(
    val rows: List<DayRow> = emptyList(),
    val loaded: Boolean = false,
    val message: String? = null,
)

class ScheduleViewModel(
    private val scope: CoroutineScope,
    private val repository: ScheduleRepository,
    private val save: SaveWeeklyScheduleUseCase,
) {
    private val _state = MutableStateFlow(ScheduleState())
    val state: StateFlow<ScheduleState> = _state.asStateFlow()

    fun load() = scope.launch {
        val existing = repository.getSchedules().associateBy { it.dayOfWeek }
        val rows = DayOfWeek.entries.map { day ->
            val s = existing[day]
            DayRow(
                day = day,
                enabled = s?.enabled ?: false,
                start = (s?.startTime ?: LocalTime.of(9, 0)).format(FMT),
                end = (s?.endTime ?: LocalTime.of(17, 0)).format(FMT),
                interval = (s?.intervalMinutes ?: 20).toString(),
            )
        }
        _state.value = ScheduleState(rows = rows, loaded = true)
    }

    fun update(day: DayOfWeek, change: (DayRow) -> DayRow) = _state.update { st ->
        st.copy(rows = st.rows.map { if (it.day == day) change(it).copy(error = null) else it }, message = null)
    }

    fun save() {
        val validated = _state.value.rows.map { validate(it) }
        if (validated.any { it.second != null }) {
            _state.update { st -> st.copy(rows = validated.map { (row, err) -> row.copy(error = err) }) }
            return
        }
        val routine = validated.map { (row, _) ->
            WeeklySchedule(
                dayOfWeek = row.day,
                startTime = LocalTime.parse(row.start.trim(), FMT),
                endTime = LocalTime.parse(row.end.trim(), FMT),
                intervalMinutes = row.interval.trim().toInt(),
                enabled = row.enabled,
            )
        }
        scope.launch {
            save(routine)
            _state.update { it.copy(message = Strings.SAVED) }
        }
    }

    companion object {
        private val FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")

        /** Disabled days are stored as-is but still need parseable values. */
        fun validate(row: DayRow): Pair<DayRow, String?> {
            val start = parse(row.start) ?: return row to Strings.ERR_TIME
            val end = parse(row.end) ?: return row to Strings.ERR_TIME
            if (!start.isBefore(end)) return row to Strings.ERR_ORDER
            val interval = row.interval.trim().toIntOrNull()
            if (interval == null || interval !in 1..240) return row to Strings.ERR_INTERVAL
            return row to null
        }

        private fun parse(text: String): LocalTime? = try {
            LocalTime.parse(text.trim(), FMT)
        } catch (_: DateTimeParseException) {
            null
        }
    }
}

@Composable
fun ScheduleScreen(container: AppContainer) {
    val scope = rememberCoroutineScope()
    val vm = remember { ScheduleViewModel(scope, container.scheduleRepository, container.saveWeeklySchedule) }
    LaunchedEffect(Unit) { vm.load() }
    val state by vm.state.collectAsState()

    Page(
        title = Strings.ROUTINE_TITLE,
        onBack = { container.navigator.home() },
        footer = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = vm::save, enabled = state.loaded) { Text(Strings.SAVE) }
                state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            }
        },
    ) {
        Text(Strings.ROUTINE_HELP, style = MaterialTheme.typography.bodyMedium)
        state.rows.forEach { row -> DayRowCard(row, onChange = { change -> vm.update(row.day, change) }) }
    }
}

@Composable
private fun DayRowCard(row: DayRow, onChange: ((DayRow) -> DayRow) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(Strings.dayName(row.day), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Switch(checked = row.enabled, onCheckedChange = { on -> onChange { it.copy(enabled = on) } })
            }
            if (row.enabled) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = row.start, onValueChange = { v -> onChange { it.copy(start = v) } },
                        label = { Text(Strings.FROM) }, singleLine = true, modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = row.end, onValueChange = { v -> onChange { it.copy(end = v) } },
                        label = { Text(Strings.TO) }, singleLine = true, modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = row.interval, onValueChange = { v -> onChange { it.copy(interval = v.filter(Char::isDigit).take(3)) } },
                        label = { Text(Strings.EVERY_MINUTES) }, singleLine = true, modifier = Modifier.weight(1.2f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
            }
            row.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
