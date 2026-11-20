package com.eyecare

import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.application
import com.eyecare.database.DatabaseFactory
import com.eyecare.di.AppContainer
import com.eyecare.presentation.ExerciseRun
import com.eyecare.presentation.Fmt
import com.eyecare.presentation.Tab
import com.eyecare.presentation.exercise.ExerciseSessionViewModel.State
import com.eyecare.presentation.floating.ExerciseContent
import com.eyecare.presentation.floating.SuccessContent
import com.eyecare.presentation.floating.ToastContent
import com.eyecare.presentation.panel.OnboardingContent
import com.eyecare.presentation.panel.PanelContent
import com.eyecare.presentation.ui.EC
import com.eyecare.platform.window.CornerWindow
import com.eyecare.platform.window.usableBounds
import com.eyecare.resources.Res
import com.eyecare.resources.app_icon
import com.eyecare.resources.tray_icon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import org.jetbrains.compose.resources.painterResource

private const val PANEL_WIDTH = 440
private const val PANEL_MAX_HEIGHT = 790

fun main() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Swing)
    val container = AppContainer(DatabaseFactory.create(), appScope)
    val alreadySetUp = runBlocking {
        container.initialize()
        container.settingsRepository.getSettings().setupAt != null
    }
    // Starting the app is an explicit action: show the panel so it's clear it runs (in the tray).
    if (alreadySetUp) container.ui.openPanel(Tab.HOME)

    application {
        val ui by container.ui.state.collectAsState()
        val settings by container.settingsRepository.observeSettings().collectAsState(null)
        val sched by container.scheduler.state.collectAsState()
        val appIcon = painterResource(Res.drawable.app_icon)
        val guard = remember { PanelToggleGuard() }

        // The scheduler lives as long as the application, also while only the tray icon is shown.
        LaunchedEffect(Unit) { container.scheduler.run() }

        val setUp = settings?.setupAt != null
        val nextText = when {
            !setUp -> "—"
            sched.dueNow -> "most"
            else -> sched.next?.let { Fmt.dayPrefixed(it.at, container.clock.now(), container.clock.zone(), omitToday = true) } ?: "—"
        }

        Tray(
            icon = painterResource(Res.drawable.tray_icon),
            tooltip = "EyeCare",
            onAction = { if (setUp && guard.mayToggle()) container.ui.togglePanel() },
            menu = {
                Item("EyeCare · következő szünet: $nextText", enabled = false, onClick = {})
                Separator()
                Item("Vezérlőpult megnyitása", enabled = setUp, onClick = { container.ui.openPanel(Tab.HOME) })
                Item("Gyakorlat indítása most", enabled = setUp, onClick = { container.ui.startExercise() })
                Item("Heti rutin", enabled = setUp, onClick = { container.ui.openPanel(Tab.SCHEDULE) })
                Separator()
                Item("Kilépés az EyeCare-ből", onClick = { container.ui.quit(::exitApplication) })
            },
        )

        val panelHeight = minOf(PANEL_MAX_HEIGHT, usableBounds().height - 20).dp

        if (settings != null && !setUp) {
            CornerWindow(
                title = "EyeCare – Első indítás", width = PANEL_WIDTH.dp, height = panelHeight, icon = appIcon,
                alwaysOnTop = false, onCloseRequest = {},
            ) {
                OnboardingContent(container, ui, onMinimize = { window.isMinimized = true })
            }
        }

        // Always composed (hidden when closed): a tray icon alone does not keep a Compose app alive.
        CornerWindow(
            title = "EyeCare", width = PANEL_WIDTH.dp, height = panelHeight, icon = appIcon,
            alwaysOnTop = true,
            visible = setUp && ui.panelOpen,
            onCloseRequest = { container.ui.closePanel() },
            onFocusLost = {
                // Like a tray flyout: clicking elsewhere hides it (the app keeps running in the tray).
                if (container.ui.state.value.panelOpen) {
                    guard.hidden()
                    container.ui.closePanel()
                }
            },
        ) {
            LaunchedEffect(Unit) {
                window.rootPane.registerKeyboardAction(
                    { container.ui.closePanel() },
                    javax.swing.KeyStroke.getKeyStroke("ESCAPE"),
                    javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW,
                )
            }
            val open = setUp && ui.panelOpen
            LaunchedEffect(open) {
                if (open) {
                    window.toFront()
                    window.requestFocus()
                }
            }
            // Recreated on every opening: fresh tab state and the next banner in rotation.
            if (open) PanelContent(container, ui, onMinimize = { container.ui.closePanel() })
        }

        ui.exercise?.let { run -> key(run.run) { ExerciseWindow(container, run, appIcon) } }

        ui.success?.let { feedback ->
            CornerWindow(
                title = "Szép munka!", width = 400.dp, height = null, icon = appIcon, alwaysOnTop = true,
                onCloseRequest = { container.ui.closeSuccess() },
            ) {
                SuccessContent(feedback, onClose = { container.ui.closeSuccess() })
            }
        }

        val toast = ui.toast
        if (toast != null && ui.exercise == null && ui.success == null) {
            key(toast.reminderId, toast.isSnoozeRepeat) {
                val shownAt = remember { Fmt.hm(container.clock.now(), container.clock.zone()) }
                CornerWindow(
                    title = "EyeCare", width = 364.dp, height = null, icon = appIcon, alwaysOnTop = true, focusable = false,
                    radius = 8.dp, background = EC.toastBg, border = EC.toastBorder,
                    onCloseRequest = { container.ui.toastClose() },
                ) {
                    ToastContent(
                        event = toast,
                        nowText = shownAt,
                        snoozeMinutes = settings?.snoozeMinutes ?: 10,
                        zone = container.clock.zone(),
                        onStart = { container.ui.toastStart() },
                        onSnooze = { container.ui.toastSnooze() },
                        onClose = { container.ui.toastClose() },
                    )
                }
            }
        }
    }
}

@Composable
private fun ApplicationScope.ExerciseWindow(container: AppContainer, run: ExerciseRun, icon: androidx.compose.ui.graphics.painter.Painter) {
    val scope = rememberCoroutineScope()
    val vm = remember { container.exerciseSession(scope, run.reminderId).also { it.start() } }
    val state by vm.state.collectAsState()
    LaunchedEffect(state) {
        when (val s = state) {
            is State.Finished -> container.ui.exerciseFinished(s.feedback)
            is State.Aborted, is State.Unavailable -> container.ui.exerciseAborted(run.reminderId)
            else -> Unit
        }
    }
    CornerWindow(
        title = "Szemtorna", width = 400.dp, height = null, icon = icon, alwaysOnTop = true,
        onCloseRequest = { vm.abort() },
    ) {
        ExerciseContent(vm, onAbort = { vm.abort() }, dragArea = { content -> WindowDraggableArea { content() } })
    }
}

/**
 * Clicking the tray icon while the panel is open first takes focus away from the panel (which hides
 * it); that same click must not immediately reopen it.
 */
private class PanelToggleGuard {
    private var hiddenAt = 0L
    fun hidden() {
        hiddenAt = System.currentTimeMillis()
    }
    fun mayToggle() = System.currentTimeMillis() - hiddenAt > 400
}
