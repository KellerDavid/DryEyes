package com.eyecare.presentation.reminder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eyecare.core.notification.ReminderEvent
import com.eyecare.presentation.Strings

/** Content of the small, non-blocking reminder popup with the two actions from section 3.3. */
@Composable
fun ReminderCard(
    event: ReminderEvent,
    snoozeMinutes: Int,
    onStart: () -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (event.isSnoozeRepeat) Strings.REMINDER_SNOOZED_TITLE else Strings.REMINDER_TITLE,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onDismiss) { Text(Strings.REMINDER_CLOSE) }
            }
            Text(Strings.REMINDER_BODY, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onStart, modifier = Modifier.weight(1f)) { Text(Strings.REMINDER_START) }
                OutlinedButton(onClick = onSnooze, modifier = Modifier.weight(1f)) { Text(Strings.snooze(snoozeMinutes)) }
            }
        }
    }
}
