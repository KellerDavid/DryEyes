package com.eyecare.platform.notification

import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.TrayState
import com.eyecare.core.notification.NotificationManager
import com.eyecare.core.notification.ReminderEvent
import com.eyecare.presentation.Strings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Desktop presentation of a reminder:
 * 1. a native OS notification through the system tray (Windows toast, macOS Notification Center,
 *    Linux notification daemon) to get attention;
 * 2. a small corner popup window carrying the two actions (start / snooze), because AWT tray
 *    notifications cannot have action buttons on every platform.
 */
class DesktopNotificationManager(val trayState: TrayState = TrayState()) : NotificationManager {

    private val _activeReminder = MutableStateFlow<ReminderEvent?>(null)
    val activeReminder: StateFlow<ReminderEvent?> = _activeReminder.asStateFlow()

    override fun showReminder(event: ReminderEvent) {
        _activeReminder.value = event
        runCatching {
            trayState.sendNotification(
                Notification(
                    title = if (event.isSnoozeRepeat) Strings.REMINDER_SNOOZED_TITLE else Strings.REMINDER_TITLE,
                    message = Strings.REMINDER_BODY,
                    type = Notification.Type.Info,
                ),
            )
        }
    }

    override fun dismissReminder() {
        _activeReminder.value = null
    }
}
