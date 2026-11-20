package com.eyecare.presentation.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eyecare.core.advertisement.AdPlacement
import com.eyecare.di.AppContainer
import com.eyecare.presentation.Tab
import com.eyecare.presentation.UiState
import com.eyecare.presentation.ui.AdStrip
import com.eyecare.presentation.ui.Btn
import com.eyecare.presentation.ui.BtnStyle
import com.eyecare.presentation.ui.Card
import com.eyecare.presentation.ui.CardTitle
import com.eyecare.presentation.ui.EC
import com.eyecare.presentation.ui.Icon
import com.eyecare.presentation.ui.Ico
import com.eyecare.presentation.ui.Logo
import com.eyecare.presentation.ui.T

/** Window title bar of the demo: logo, name, minimise and close buttons. */
@Composable
fun TitleBar(title: String, onMinimize: () -> Unit, onClose: () -> Unit, closeHint: Boolean = true) {
    Row(
        Modifier.fillMaxWidth().height(40.dp).background(EC.nav).padding(start = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Logo()
        Spacer(Modifier.width(10.dp))
        T(title, 13.sp, weight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Box(Modifier.width(46.dp).fillMaxHeight().clickable(onClick = onMinimize), contentAlignment = Alignment.Center) { T("—", 14.sp, EC.muted) }
        Box(Modifier.width(46.dp).fillMaxHeight().clickable(enabled = closeHint, onClick = onClose), contentAlignment = Alignment.Center) { T("✕", 14.sp, EC.muted) }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(EC.line))
}

/** Bottom action bar (Mégse / Mentés). */
@Composable
fun ActionBar(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(EC.line))
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
    ) { content() }
}

@Composable
fun ColumnScope.ScrollArea(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp), content = content)
}

/** The tray panel ("vezérlőpult"): tabs, optional action bar, ad strip and bottom navigation. */
@Composable
fun PanelContent(c: AppContainer, ui: UiState, onMinimize: () -> Unit) {
    val banner = remember { c.adManager.bannerFor(AdPlacement.PANEL) }
    Column(Modifier.fillMaxSize().background(EC.bg)) {
        TitleBar("EyeCare", onMinimize = onMinimize, onClose = { c.ui.closePanel() })
        val draft = ui.draft.takeIf { ui.tab == Tab.SCHEDULE }
        ScrollArea {
            when {
                ui.tab == Tab.SETTINGS -> SettingsTab(c)
                ui.tab == Tab.SCHEDULE && draft != null -> ScheduleEdit(c, draft)
                ui.tab == Tab.SCHEDULE -> ScheduleList(c)
                else -> HomeTab(c)
            }
        }
        if (draft != null) {
            ActionBar {
                Btn("Mégse", { c.ui.cancelEdit() }, style = BtnStyle.GHOST)
                Btn(
                    if (draft.isNew) "Hozzáadás" else "Mentés", { c.ui.saveDraft() },
                    style = BtnStyle.PRIMARY, icon = Ico.CHECK, enabled = draft.band.days.isNotEmpty(),
                )
            }
        }
        if (banner != null) AdStrip(banner, Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp))
        BottomNav(ui.tab) { tab -> if (tab == Tab.SCHEDULE) c.ui.openPanel(Tab.SCHEDULE) else c.ui.selectTab(tab) }
    }
}

@Composable
private fun BottomNav(current: Tab, onSelect: (Tab) -> Unit) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(EC.line))
    Row(
        Modifier.fillMaxWidth().background(EC.nav).padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        listOf(Triple(Tab.HOME, Ico.HOME, "Főoldal"), Triple(Tab.SCHEDULE, Ico.CAL, "Heti rutin"), Triple(Tab.SETTINGS, Ico.GEAR, "Beállítások"))
            .forEach { (tab, icon, label) ->
                val on = tab == current
                val color = if (on) EC.primary else EC.muted
                Column(
                    Modifier.widthIn(min = 96.dp).clip(RoundedCornerShape(12.dp)).clickable { onSelect(tab) }
                        .padding(start = 18.dp, end = 18.dp, top = 7.dp, bottom = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        Modifier.size(52.dp, 28.dp).clip(RoundedCornerShape(14.dp)).background(if (on) EC.primarySoft else Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) { Icon(icon, 21.dp, color) }
                    T(label, 11.5.sp, color, FontWeight.SemiBold)
                }
            }
    }
}

/** First launch: the routine must be set before the app starts reminding. */
@Composable
fun OnboardingContent(c: AppContainer, ui: UiState, onMinimize: () -> Unit) {
    val band = ui.onboardingDraft
    Column(Modifier.fillMaxSize().background(EC.bg)) {
        TitleBar("EyeCare – Első indítás", onMinimize = onMinimize, onClose = {}, closeHint = false)
        ScrollArea {
            Row(Modifier.padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Logo(52.dp, 30.dp, 15.dp)
                Column {
                    T("Üdv az EyeCare-ben!", 21.sp, weight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 3.dp))
                    T(
                        "A beállított időpontokban rövid szemtornára emlékeztetünk. Az adataid csak ezen a gépen tárolódnak.",
                        12.5.sp, EC.muted, lineHeight = 18.sp,
                    )
                }
            }
            Card {
                CardTitle("Mikor kérsz emlékeztetőt?", Ico.CAL)
                BandFields(band, showActive = false) { change -> c.ui.updateOnboarding(change) }
            }
            T(
                "Később a Heti rutin fülön bármikor módosíthatod. Bezáráskor az app a tálcán fut tovább.",
                12.sp, EC.muted, lineHeight = 17.sp, modifier = Modifier.padding(top = 14.dp),
            )
        }
        ActionBar {
            Btn("Mentés és indítás", { c.ui.completeOnboarding() }, style = BtnStyle.PRIMARY, icon = Ico.CHECK, large = true, full = true, enabled = band.days.isNotEmpty())
        }
    }
}
