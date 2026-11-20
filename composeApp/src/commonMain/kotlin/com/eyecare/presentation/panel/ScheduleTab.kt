package com.eyecare.presentation.panel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eyecare.di.AppContainer
import com.eyecare.domain.model.TimeBand
import com.eyecare.presentation.BandDraft
import com.eyecare.presentation.Fmt
import com.eyecare.presentation.ui.Btn
import com.eyecare.presentation.ui.BtnStyle
import com.eyecare.presentation.ui.Card
import com.eyecare.presentation.ui.DayPick
import com.eyecare.presentation.ui.EC
import com.eyecare.presentation.ui.Field
import com.eyecare.presentation.ui.Heading
import com.eyecare.presentation.ui.Icon
import com.eyecare.presentation.ui.Ico
import com.eyecare.presentation.ui.Segmented
import com.eyecare.presentation.ui.Summary
import com.eyecare.presentation.ui.Switch
import com.eyecare.presentation.ui.T
import com.eyecare.presentation.ui.TimeSelect
import kotlinx.coroutines.delay
import java.time.DayOfWeek

@Composable
fun ScheduleList(c: AppContainer) {
    val bands by c.scheduleRepository.observeBands().collectAsState(emptyList())
    val sched by c.scheduler.state.collectAsState()
    val now by produceState(c.clock.now()) {
        while (true) {
            delay(30_000)
            value = c.clock.now()
        }
    }
    val zone = c.clock.zone()

    Heading("Heti rutin", "Ezekben az idősávokban kapsz emlékeztetőt. Kattints egy sávra a szerkesztéshez.")
    Card {
        WeekCalendar(bands, now.atZone(zone).let { it.dayOfWeek to (it.hour * 60 + it.minute) }, 270.dp) { c.ui.editBand(it) }
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Legend({ Box(Modifier.size(12.dp, 10.dp).clip(RoundedCornerShape(3.dp)).background(EC.band)) }, "Idősáv")
            Legend({ Box(Modifier.size(6.dp).clip(CircleShape).background(EC.bandInk)) }, "Emlékeztető")
            Legend({ Box(Modifier.size(12.dp, 2.dp).clip(RoundedCornerShape(1.dp)).background(EC.accent)) }, "Most")
        }
    }
    bands.forEach { band -> BandRow(band) { c.ui.editBand(band) } }
    if (bands.isEmpty()) T("Még nincs idősáv, így nem kapsz emlékeztetőt.", 12.sp, EC.muted, modifier = Modifier.padding(top = 14.dp))
    Spacer(Modifier.height(10.dp))
    Btn("Új idősáv", { c.ui.newBand() }, icon = Ico.PLUS, full = true)
    Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Ico.BELL, 18.dp, EC.primary)
        val next = sched.next
        if (sched.dueNow || next != null) {
            val label = if (sched.dueNow) "most" else Fmt.dayPrefixed(next!!.at, now, zone, omitToday = false).replaceFirstChar { it.lowercase() }
            androidx.compose.material3.Text(
                buildAnnotatedString {
                    append("Következő értesítés: ")
                    pushStyle(SpanStyle(color = EC.text, fontWeight = FontWeight.Bold)); append(label); pop()
                },
                style = TextStyle(fontSize = 12.5.sp, color = EC.muted),
            )
        } else {
            T("Nincs ütemezett értesítés", 12.5.sp, EC.muted)
        }
    }
}

@Composable
private fun Legend(swatch: @Composable () -> Unit, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        swatch()
        T(label, 11.5.sp, EC.muted)
    }
}

@Composable
private fun BandRow(band: TimeBand, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier.padding(top = 10.dp).fillMaxWidth().alpha(if (band.active) 1f else 0.55f)
            .clip(shape).background(EC.surface).border(1.dp, EC.line, shape).clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(EC.band), contentAlignment = Alignment.Center) {
            Icon(Ico.CAL, 18.dp, EC.bandInk)
        }
        Column(Modifier.weight(1f)) {
            T(
                "${Fmt.daysText(band.days)} · ${Fmt.minutes(band.startMinute)}–${Fmt.minutes(band.endMinute)}" + if (band.crossesMidnight) " (+1)" else "",
                13.5.sp, weight = FontWeight.Bold,
            )
            T(
                "${Fmt.intervalLabel(band.intervalMinutes)} · ${band.reminderCount} emlékeztető" + if (band.active) "" else " · kikapcsolva",
                12.sp, EC.muted,
            )
        }
        T("›", 18.sp, EC.muted)
    }
}

/** Week grid: 7 day columns × 24 h, bands as blocks, reminder ticks and a "now" line on today. */
@Composable
private fun WeekCalendar(bands: List<TimeBand>, now: Pair<DayOfWeek, Int>, height: Dp, onSelect: (TimeBand) -> Unit) {
    data class Piece(val band: TimeBand, val from: Int, val to: Int, val ticks: List<Int>)
    val columns = List(7) { mutableListOf<Piece>() }
    bands.forEach { b ->
        val ticks = (0 until b.reminderCount).map { b.startMinute + it * b.intervalMinutes }
        b.days.forEach { d ->
            val i = d.value - 1
            if (b.crossesMidnight) {
                columns[i] += Piece(b, b.startMinute, 1440, ticks.filter { it < 1440 })
                if (b.endMinute > 0) columns[(i + 1) % 7] += Piece(b, 0, b.endMinute, ticks.filter { it >= 1440 }.map { it - 1440 })
            } else {
                columns[i] += Piece(b, b.startMinute, b.endMinute, ticks)
            }
        }
    }
    val today = now.first.value - 1

    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Spacer(Modifier.width(30.dp))
            Fmt.DAY_SHORT.forEachIndexed { i, d ->
                T(d, 11.5.sp, if (i == today) EC.primary else EC.muted, FontWeight.Bold, Modifier.weight(1f).padding(bottom = 6.dp), align = TextAlign.Center)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.width(30.dp).height(height)) {
                listOf(0, 6, 9, 12, 15, 18, 24).forEach { h ->
                    T(
                        h.toString().padStart(2, '0'), 10.sp, EC.muted, tabular = true,
                        modifier = Modifier.align(Alignment.TopEnd).offset(y = height * (h / 24f) - 6.dp).padding(end = 4.dp),
                    )
                }
            }
            columns.forEachIndexed { i, pieces ->
                Box(Modifier.weight(1f).height(height)) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawRoundRect(EC.surface2, cornerRadius = CornerRadius(6.dp.toPx()))
                        for (k in 1 until 8) {
                            val y = size.height * k / 8f
                            drawRect(EC.line, Offset(0f, y - 1f), Size(size.width, 1f))
                        }
                    }
                    pieces.forEach { p ->
                        val shape = RoundedCornerShape(6.dp)
                        Box(
                            Modifier.offset(y = height * (p.from / 1440f)).height(height * ((p.to - p.from) / 1440f))
                                .fillMaxWidth().padding(horizontal = 2.dp).alpha(if (p.band.active) 1f else 0.4f)
                                .clip(shape).background(EC.band).border(1.5.dp, EC.bandInk.copy(alpha = 0.35f), shape)
                                .clickable { onSelect(p.band) },
                        )
                        if (p.band.active) p.ticks.forEach { t ->
                            Box(
                                Modifier.align(Alignment.TopCenter).offset(y = height * (t / 1440f) - 2.5.dp)
                                    .size(5.dp).clip(CircleShape).background(EC.bandInk),
                            )
                        }
                    }
                    if (i == today) {
                        Box(
                            Modifier.offset(y = height * (now.second / 1440f) - 1.dp).fillMaxWidth().height(2.dp)
                                .clip(RoundedCornerShape(2.dp)).background(EC.accent),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleEdit(c: AppContainer, draft: BandDraft) {
    Row(
        Modifier.padding(bottom = 10.dp).clickable { c.ui.cancelEdit() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Ico.LEFT, 15.dp, EC.primary)
        T("Heti rutin", 12.5.sp, EC.primary, FontWeight.SemiBold)
    }
    Heading(if (draft.isNew) "Új idősáv" else "Idősáv szerkesztése")
    Card {
        BandFields(draft.band, showActive = true) { change -> c.ui.updateDraft(change) }
        if (!draft.isNew) {
            Spacer(Modifier.height(8.dp))
            Btn("Idősáv törlése", { c.ui.deleteDraft() }, style = BtnStyle.DANGER, icon = Ico.TRASH)
        }
    }
}

/** Day picker, start/end, interval, optional active toggle and the summary (shared with onboarding). */
@Composable
fun BandFields(band: TimeBand, showActive: Boolean, onChange: ((TimeBand) -> TimeBand) -> Unit) {
    Field("Napok") {
        DayPick(Fmt.DAY_SHORT, selected = { DayOfWeek.of(it + 1) in band.days }) { i ->
            val d = DayOfWeek.of(i + 1)
            onChange { b -> b.copy(days = if (d in b.days) b.days - d else b.days + d) }
        }
    }
    Field("Idősáv") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimeSelect("Kezdés", band.startMinute, { m -> onChange { it.copy(startMinute = m) } }, "", Modifier.weight(1f))
            TimeSelect("Befejezés", band.endMinute, { m -> onChange { it.copy(endMinute = m) } }, if (band.crossesMidnight) "másnap" else "", Modifier.weight(1f))
        }
    }
    Field("Emlékeztető gyakorisága") {
        Segmented(TimeBand.INTERVAL_OPTIONS.map { it to Fmt.intervalOption(it) }, band.intervalMinutes) { v -> onChange { it.copy(intervalMinutes = v) } }
    }
    if (showActive) {
        Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                T("Aktív", 13.5.sp)
                T("Kikapcsolva megmarad, de nem jelez", 11.5.sp, EC.muted, modifier = Modifier.padding(top = 2.dp))
            }
            Switch(band.active) { onChange { it.copy(active = !it.active) } }
        }
    }
    val summary = Fmt.summary(band)
    Summary(summary.text, summary.bad)
}
