package com.eyecare.presentation.panel

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eyecare.di.AppContainer
import com.eyecare.domain.model.UserSettings
import com.eyecare.presentation.ui.Card
import com.eyecare.presentation.ui.CardTitle
import com.eyecare.presentation.ui.Choice
import com.eyecare.presentation.ui.EC
import com.eyecare.presentation.ui.Heading
import com.eyecare.presentation.ui.Ico
import com.eyecare.presentation.ui.Info
import com.eyecare.presentation.ui.T
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsTab(c: AppContainer) {
    val scope = rememberCoroutineScope()
    val settings by c.settingsRepository.observeSettings().collectAsState(null)
    var savedTick by remember { mutableStateOf(0) }
    var showSaved by remember { mutableStateOf(false) }
    LaunchedEffect(savedTick) {
        if (savedTick > 0) {
            showSaved = true
            delay(3_000)
            showSaved = false
        }
    }

    Heading("Beállítások", "Minden adat ezen a gépen marad.")
    Card {
        CardTitle("Szundi időtartama", Ico.ZZ)
        val current = settings?.snoozeMinutes ?: UserSettings.DEFAULT_SNOOZE_MINUTES
        Choice(UserSettings.SNOOZE_OPTIONS.map { it to "$it perc" }, current) { v ->
            scope.launch {
                val s = c.settingsRepository.getSettings()
                c.settingsRepository.saveSettings(s.copy(snoozeMinutes = v))
                savedTick++
            }
        }
        T("Ennyi idő múlva jelez újra az elhalasztott emlékeztető.", 11.5.sp, EC.muted, modifier = Modifier.padding(top = 8.dp))
    }
    Spacer(Modifier.height(12.dp))
    Card {
        CardTitle("Működés", Ico.TRAY)
        Info(Ico.TRAY, "Bezáráskor a tálcán fut tovább", ", így az emlékeztetők megérkeznek. Teljes kilépés: tálcaikon menü → Kilépés.")
        Info(Ico.LOCK, "Offline, helyi adattárolás.", " Nincs regisztráció, nincs felhő.")
    }
    T(if (showSaved) "✓ Mentve" else "", 11.5.sp, EC.ok, modifier = Modifier.fillMaxWidth().height(24.dp).padding(top = 8.dp), align = TextAlign.End)
}
