package com.eyecare.platform.window

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.eyecare.core.notification.ReminderEvent
import com.eyecare.presentation.Strings
import com.eyecare.presentation.reminder.ReminderCard
import com.eyecare.presentation.theme.EyeCareTheme
import com.eyecare.domain.model.AppTheme

/**
 * Small reminder window in the bottom-right corner of the primary screen.
 * It never takes keyboard focus (typing in other apps continues) and covers only a corner;
 * it stays on top so it isn't lost behind the active window, and goes away after either action.
 */
@Composable
fun ReminderPopupWindow(
    event: ReminderEvent,
    theme: AppTheme,
    snoozeMinutes: Int,
    onStart: () -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberWindowState(
        position = WindowPosition(Alignment.BottomEnd),
        size = DpSize(400.dp, 170.dp),
    )
    Window(
        onCloseRequest = onDismiss,
        state = state,
        title = Strings.APP_NAME,
        undecorated = true,
        transparent = true,
        resizable = false,
        focusable = false,
        alwaysOnTop = true,
    ) {
        EyeCareTheme(theme) {
            ReminderCard(event, snoozeMinutes, onStart, onSnooze, onDismiss)
        }
    }
}
