package com.eyecare.snapshots

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.use
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.eyecare.core.gamification.Feedback
import com.eyecare.core.notification.ReminderEvent
import com.eyecare.data.database.AppDatabase
import com.eyecare.di.AppContainer
import com.eyecare.domain.model.NotificationStatus
import com.eyecare.domain.model.UserSettings
import com.eyecare.presentation.BandDraft
import com.eyecare.presentation.Tab
import com.eyecare.presentation.UiState
import com.eyecare.presentation.floating.ExerciseContent
import com.eyecare.presentation.floating.SuccessContent
import com.eyecare.presentation.floating.ToastContent
import com.eyecare.presentation.panel.OnboardingContent
import com.eyecare.presentation.panel.PanelContent
import com.eyecare.presentation.ui.EC
import com.eyecare.presentation.ui.EyeCareTheme
import com.eyecare.testing.BUDAPEST
import com.eyecare.testing.FakeClock
import com.eyecare.testing.band
import com.eyecare.testing.local
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test

/**
 * Renders the main surfaces offscreen to PNG for visual comparison with the clickable demo.
 * Runs only when DRYEYES_SNAPSHOT_DIR is set: `DRYEYES_SNAPSHOT_DIR=/tmp/snap ./gradlew desktopTest`.
 */
class SnapshotRender {
    private val outDir = System.getenv("DRYEYES_SNAPSHOT_DIR")?.let(::File)

    private fun container(): AppContainer {
        val db = Room.inMemoryDatabaseBuilder<AppDatabase>().setDriver(BundledSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO).build()
        val clock = FakeClock(local("2026-11-23T12:20"), BUDAPEST)
        return AppContainer(db, CoroutineScope(SupervisorJob() + Dispatchers.Default), clock)
    }

    private fun shot(name: String, width: Int, height: Int, content: @Composable () -> Unit) {
        val dir = outDir ?: return
        dir.mkdirs()
        ImageComposeScene(width * 2, height * 2, Density(2f)) {
            EyeCareTheme { content() }
        }.use { scene ->
            var t = 0L
            repeat(6) {
                scene.render(t)
                Thread.sleep(150)
                t += 300_000_000L
            }
            val img = scene.render(t)
            File(dir, "$name.png").writeBytes(img.encodeToData(EncodedImageFormat.PNG)!!.bytes)
        }
    }

    @Composable
    private fun Frame(width: Int?, content: @Composable () -> Unit) {
        val shape = RoundedCornerShape(12.dp)
        Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFF0E2147))) {
            Box((if (width != null) Modifier.width(width.dp) else Modifier.fillMaxSize()).clip(shape).background(EC.bg).border(1.dp, EC.winBorder, shape)) { content() }
        }
    }

    @Test
    fun render() {
        if (outDir == null) return
        val c = container()
        runBlocking {
            c.initialize()
            c.saveBand(band(id = 0))
            c.settingsRepository.saveSettings(UserSettings(10, local("2026-11-20T08:00")))
            c.reminderRepository.record(1, local("2026-11-23T09:00"), NotificationStatus.DONE)
            val snz = c.reminderRepository.record(1, local("2026-11-23T10:30"), NotificationStatus.SHOWN)
            c.reminderRepository.snooze(snz, local("2026-11-23T10:40"))
            c.reminderRepository.setStatus(snz, NotificationStatus.DONE)
            c.reminderRepository.record(1, local("2026-11-23T12:00"), NotificationStatus.MISSED)
            c.exerciseRepository.recordCompletion(c.exerciseRepository.getDefaultExercise()!!.id, null, local("2026-11-23T11:15"))
            c.scheduler.evaluate()
        }
        val base = UiState(panelOpen = true)
        shot("panel_home", 440, 790) { Frame(null) { PanelContent(c, base) {} } }
        shot("panel_schedule", 440, 790) { Frame(null) { PanelContent(c, base.copy(tab = Tab.SCHEDULE)) {} } }
        shot("panel_edit", 440, 790) {
            Frame(null) { PanelContent(c, base.copy(tab = Tab.SCHEDULE, draft = BandDraft(band(), isNew = false))) {} }
        }
        shot("panel_settings", 440, 790) { Frame(null) { PanelContent(c, base.copy(tab = Tab.SETTINGS)) {} } }
        shot("onboarding", 440, 790) { Frame(null) { OnboardingContent(c, UiState()) {} } }
        shot("toast", 364, 200) {
            ToastContent(ReminderEvent(1, 1, local("2026-11-23T10:30"), false), "12:20", 10, BUDAPEST, {}, {}, {})
        }
        shot("success", 400, 470) { Frame(400) { SuccessContent(Feedback(3, 6)) {} } }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val vm = c.exerciseSession(scope, null).also { it.start() }
        Thread.sleep(2_300)
        shot("exercise", 400, 600) { Frame(400) { ExerciseContent(vm, {}) { it() } } }
    }
}
