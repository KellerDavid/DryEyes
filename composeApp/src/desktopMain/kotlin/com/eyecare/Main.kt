package com.eyecare

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.eyecare.database.DatabaseFactory
import com.eyecare.di.AppContainer
import com.eyecare.domain.model.UserSettings
import com.eyecare.platform.notification.DesktopNotificationManager
import com.eyecare.platform.window.ReminderPopupWindow
import com.eyecare.presentation.App
import com.eyecare.presentation.Strings
import com.eyecare.resources.Res
import com.eyecare.resources.app_icon
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.painterResource
import java.awt.Window as AwtWindow

fun main() {
    val notifier = DesktopNotificationManager()
    val container = AppContainer(database = DatabaseFactory.create(), notifier = notifier)
    runBlocking { container.initialize() }

    application {
        val scope = rememberCoroutineScope()
        val icon = painterResource(Res.drawable.app_icon)
        var mainVisible by remember { mutableStateOf(true) }
        val settings by container.settingsRepository.observeSettings().collectAsState(UserSettings())
        val reminder by notifier.activeReminder.collectAsState()

        // The scheduler lives as long as the application, also while only the tray icon is shown.
        LaunchedEffect(Unit) { container.scheduler.run() }

        fun openMain() {
            mainVisible = true
        }

        Tray(
            icon = icon,
            state = notifier.trayState,
            tooltip = Strings.APP_NAME,
            onAction = ::openMain,
            menu = {
                Item(Strings.TRAY_OPEN, onClick = ::openMain)
                Item(Strings.TRAY_EXERCISE, onClick = {
                    container.navigator.startExercise()
                    openMain()
                })
                Separator()
                Item(Strings.TRAY_QUIT, onClick = ::exitApplication)
            },
        )

        val windowState = rememberWindowState(size = DpSize(480.dp, 680.dp), position = WindowPosition.PlatformDefault)
        Window(
            onCloseRequest = { mainVisible = false }, // close → keep running in the tray (section 8.4)
            visible = mainVisible,
            state = windowState,
            title = Strings.APP_NAME,
            icon = icon,
        ) {
            LaunchedEffect(mainVisible) { if (mainVisible) window.bringToFront() }
            App(container)
        }

        reminder?.let { event ->
            ReminderPopupWindow(
                event = event,
                theme = settings.theme,
                snoozeMinutes = settings.snoozeMinutes,
                onStart = {
                    notifier.dismissReminder()
                    container.navigator.startExercise(reminderId = event.reminderId)
                    openMain()
                },
                onSnooze = { scope.launch { container.snoozeReminder(event.reminderId) } },
                onDismiss = { scope.launch { container.dismissReminder(event.reminderId) } },
            )
        }
    }
}

private fun AwtWindow.bringToFront() {
    if (this is java.awt.Frame && extendedState == java.awt.Frame.ICONIFIED) extendedState = java.awt.Frame.NORMAL
    toFront()
    requestFocus()
}
