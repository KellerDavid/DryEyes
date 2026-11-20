package com.eyecare.presentation.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.eyecare.di.AppContainer
import com.eyecare.presentation.Tab
import com.eyecare.presentation.home.Hero
import com.eyecare.presentation.home.HomeModel
import com.eyecare.presentation.home.HomeUi
import com.eyecare.presentation.home.SlotKind
import com.eyecare.presentation.home.TimelineRow
import com.eyecare.presentation.ui.Bar
import com.eyecare.presentation.ui.Btn
import com.eyecare.presentation.ui.BtnStyle
import com.eyecare.presentation.ui.Card
import com.eyecare.presentation.ui.CardTitle
import com.eyecare.presentation.ui.Chip
import com.eyecare.presentation.ui.EC
import com.eyecare.presentation.ui.Heading
import com.eyecare.presentation.ui.Icon
import com.eyecare.presentation.ui.Ico
import com.eyecare.presentation.ui.Ring
import com.eyecare.presentation.ui.T
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.Duration

class HomeViewModel(scope: CoroutineScope, c: AppContainer) {
    private val since = c.clock.now().minus(Duration.ofDays(2))
    private val ticker = flow {
        while (true) {
            emit(c.clock.now())
            delay(250)
        }
    }

    val state: StateFlow<HomeUi?> = combine(
        c.scheduleRepository.observeBands(),
        c.reminderRepository.observeSince(since),
        c.exerciseRepository.observeCompletionsSince(since),
        combine(c.scheduler.state, c.settingsRepository.observeSettings()) { a, b -> a to b },
        ticker,
    ) { bands, rows, completions, (sched, settings), now ->
        HomeModel.build(now, c.clock.zone(), bands, rows, completions, sched, settings.snoozeMinutes, settings.setupAt)
    }.stateIn(scope, SharingStarted.Eagerly, null)
}

@Composable
fun HomeTab(c: AppContainer) {
    val scope = rememberCoroutineScope()
    val vm = remember { HomeViewModel(scope, c) }
    val ui by vm.state.collectAsState()
    when (val s = ui) {
        null -> Unit
        is HomeUi.Empty -> EmptyHome(c, s.greeting)
        is HomeUi.Routine -> RoutineHome(c, s)
    }
}

@Composable
private fun EmptyHome(c: AppContainer, greeting: String) {
    Heading(greeting, "Nincs beállított rutin")
    Card {
        Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Ico.CAL, 28.dp, EC.primary)
            Spacer(Modifier.height(8.dp))
            T("Nincs aktív idősáv, így nem kapsz emlékeztetőt.", 13.sp, EC.muted, align = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            Btn("Heti rutin beállítása", { c.ui.openPanel(Tab.SCHEDULE) }, style = BtnStyle.PRIMARY, icon = Ico.CAL)
        }
    }
    Spacer(Modifier.height(12.dp))
    Btn("Gyakorlat indítása most", { c.ui.startExercise() }, icon = Ico.PLAY, large = true, full = true)
}

@Composable
private fun RoutineHome(c: AppContainer, s: HomeUi.Routine) {
    Heading(s.greeting, s.routineLine)
    Card(brush = Brush.linearGradient(0f to EC.surface, 1f to lerp(EC.surface, EC.primarySoft, 0.67f))) {
        HeroTop(s.hero)
        Spacer(Modifier.height(14.dp))
        Btn("Gyakorlat indítása most", { c.ui.startExercise() }, style = BtnStyle.PRIMARY, icon = Ico.PLAY, large = true, full = true)
    }
    Spacer(Modifier.height(12.dp))
    Card {
        CardTitle(s.timelineTitle, Ico.CHECK)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { s.rows.forEach { SlotRow(it) } }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(EC.line))
        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            T("Teljesítve", 12.5.sp, EC.muted)
            Bar(if (s.total == 0) 0f else s.done.toFloat() / s.total, Modifier.weight(1f))
            T("${s.done} / ${s.total}", 15.sp, weight = FontWeight.Bold)
        }
    }
}

private fun lerp(a: Color, b: Color, t: Float) = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
)

@Composable
private fun HeroTop(hero: Hero) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        when (hero) {
            Hero.Due -> {
                Ring(108.dp, 1f, 8f) {
                    T("Most", 24.sp, weight = FontWeight.Bold, letterSpacing = (-0.02).em)
                    T("esedékes", 11.5.sp, EC.muted)
                }
                HeroText("Szemkímélő szünet", { T("Most", 24.sp, weight = FontWeight.SemiBold) }, "Az értesítés a jobb alsó sarokban vár rád.")
            }
            is Hero.Countdown -> {
                val secs = hero.remaining.toMillis() < 60_000
                Ring(108.dp, hero.fraction, 8f, if (hero.isSnooze) EC.warn else EC.primary) {
                    val value = if (secs) (hero.remaining.toMillis() + 999) / 1000 else (hero.remaining.toMillis() + 59_999) / 60_000
                    T(value.toString(), 30.sp, weight = FontWeight.Bold, tabular = true, letterSpacing = (-0.02).em)
                    T(if (secs) "mp múlva" else "perc múlva", 11.5.sp, EC.muted)
                }
                HeroText(
                    "Következő szemkímélő szünet",
                    {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            T(hero.label, 24.sp, weight = FontWeight.SemiBold)
                            if (hero.isSnooze) Chip("Szundi", Ico.ZZ)
                        }
                    },
                    hero.description,
                )
            }
            is Hero.Idle -> {
                Ring(108.dp, 0f, 8f) { Icon(Ico.MOON, 30.dp, EC.muted) }
                HeroText(
                    "Most nincs aktív idősáv",
                    { T(hero.nextLabel ?: "—", 24.sp, weight = FontWeight.SemiBold) },
                    "A rutinodon kívül nem kapsz emlékeztetőt. Az app csendben fut a tálcán.",
                )
            }
        }
    }
}

@Composable
private fun HeroText(lead: String, big: @Composable () -> Unit, desc: String) {
    Column {
        T(lead, 12.5.sp, EC.muted)
        Box(Modifier.padding(top = 2.dp, bottom = 4.dp)) { big() }
        T(desc, 12.5.sp, EC.muted, lineHeight = 18.sp)
    }
}

@Composable
private fun SlotRow(row: TimelineRow) {
    val strong = row.kind == SlotKind.NEXT || row.kind == SlotKind.SNOOZED
    val accentStatus = strong || row.kind == SlotKind.SNOOZE_DONE
    Row(
        Modifier.fillMaxWidth().alpha(if (row.kind == SlotKind.NOT_RUN) 0.55f else 1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        T(row.time, 13.5.sp, EC.muted, tabular = true, modifier = Modifier.width(42.dp))
        SlotDot(row.kind)
        T(row.name, 13.5.sp, weight = if (strong) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.weight(1f))
        T(row.status, 12.sp, if (accentStatus) EC.primary else EC.muted, if (accentStatus) FontWeight.SemiBold else FontWeight.Normal, align = TextAlign.End)
    }
}

@Composable
private fun SlotDot(kind: SlotKind) {
    val (fill, border, width) = when (kind) {
        SlotKind.DONE -> Triple(EC.ok, EC.ok, 2.dp)
        SlotKind.SNOOZE_DONE -> Triple(EC.warn, EC.warn, 2.dp)
        SlotKind.MISSED -> Triple(EC.miss, EC.miss, 2.dp)
        SlotKind.NEXT -> Triple(Color.Transparent, EC.primary, 2.5.dp)
        SlotKind.SNOOZED -> Triple(EC.primarySoft, EC.warn, 2.5.dp)
        SlotKind.NOT_RUN, SlotKind.PLANNED -> Triple(Color.Transparent, EC.miss, 2.dp)
    }
    Box(
        Modifier.size(20.dp).clip(CircleShape).background(fill).border(width, border, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        when (kind) {
            SlotKind.DONE, SlotKind.SNOOZE_DONE -> Icon(Ico.CHECK, 12.dp, EC.okInk, strokeWidth = 2.8f)
            SlotKind.MISSED -> Icon(Ico.X, 12.dp, EC.muted, strokeWidth = 2.8f)
            else -> Unit
        }
    }
}
