package com.eyecare.presentation.floating

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eyecare.core.gamification.Feedback
import com.eyecare.core.notification.ReminderEvent
import com.eyecare.presentation.Fmt
import com.eyecare.presentation.exercise.ExerciseSessionViewModel
import com.eyecare.presentation.exercise.ExerciseSessionViewModel.State
import com.eyecare.presentation.ui.Bar
import com.eyecare.presentation.ui.Btn
import com.eyecare.presentation.ui.BtnStyle
import com.eyecare.presentation.ui.EC
import com.eyecare.presentation.ui.Icon
import com.eyecare.presentation.ui.Ico
import com.eyecare.presentation.ui.Logo
import com.eyecare.presentation.ui.Ring
import com.eyecare.presentation.ui.StepArt
import com.eyecare.presentation.ui.SvgCanvas
import com.eyecare.presentation.ui.T
import kotlin.math.cos
import kotlin.math.sin

/** Wraps the title bar so the desktop layer can make it a window drag handle. */
typealias DragArea = @Composable (content: @Composable () -> Unit) -> Unit

/** Floating exercise window (no ads, never blocks other apps). Steps advance on their own. */
@Composable
fun ExerciseContent(vm: ExerciseSessionViewModel, onAbort: () -> Unit, dragArea: DragArea) {
    val state by vm.state.collectAsState()
    val s = state as? State.Running
    Column(Modifier.fillMaxWidth().background(EC.bg)) {
        dragArea {
            Row(
                Modifier.fillMaxWidth().height(40.dp).background(EC.nav).padding(start = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Ico.EYE, 15.dp, EC.primary)
                Spacer(Modifier.width(8.dp))
                T("Szemtorna", 13.sp, weight = FontWeight.SemiBold)
                T(if (s != null) " · ${s.stepIndex + 1} / ${s.stepCount} lépés" else "", 13.sp, EC.muted, modifier = Modifier.weight(1f))
                Box(Modifier.width(46.dp).fillMaxHeight().clickable(onClick = onAbort), contentAlignment = Alignment.Center) { T("✕", 14.sp, EC.muted) }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(EC.line))
        if (s == null) {
            Box(Modifier.fillMaxWidth().height(480.dp))
            return@Column
        }
        Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(s.stepCount) { k ->
                val color = when {
                    k < s.stepIndex -> EC.ok
                    k == s.stepIndex -> EC.primary
                    else -> EC.surface2
                }
                Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(5.dp)).background(color))
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 9.dp)) {
            T(if (s.stepIndex > 0) "✓ ${s.exercise.steps[s.stepIndex - 1].title}" else "", 12.sp, EC.ok, modifier = Modifier.weight(1f))
            T("Lépés hossza: ${s.step.durationSeconds} mp", 12.sp, EC.muted)
        }
        Box(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 4.dp), contentAlignment = Alignment.Center) {
            Ring(200.dp, s.stepFraction, 7f) {
                val (w, h) = if (s.step.art == "far") 70.dp to 42.dp else 64.dp to 38.dp
                SvgCanvas(96f, 58f, StepArt.parts(s.step.art), Modifier.size(w, h).padding(bottom = 2.dp))
                T(s.remainingSeconds.toString(), 46.sp, weight = FontWeight.Bold, tabular = true, lineHeight = 48.sp)
                T("másodperc", 11.5.sp, EC.muted)
            }
        }
        Column(Modifier.fillMaxWidth().padding(start = 30.dp, end = 30.dp, top = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            T(s.step.title, 23.sp, weight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp))
            T(s.step.instruction, 13.5.sp, EC.muted, align = TextAlign.Center, lineHeight = 20.sp)
        }
        val last = s.stepIndex == s.stepCount - 1
        Row(
            Modifier.padding(start = 22.dp, end = 22.dp, top = 16.dp).fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(EC.surface2).padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(if (last) Ico.CHECK else Ico.SKIP, 18.dp, EC.muted)
            val next = if (last) "Kész, jön a visszajelzés" else s.exercise.steps[s.stepIndex + 1].let { "${it.title} · ${it.durationSeconds} mp" }
            androidx.compose.material3.Text(
                buildAnnotatedString {
                    append("Következik: ")
                    pushStyle(SpanStyle(color = EC.text, fontWeight = FontWeight.Bold)); append(next); pop()
                },
                style = TextStyle(fontSize = 12.5.sp, color = EC.muted),
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            T("Automatikusan lép tovább", 11.5.sp, EC.muted, modifier = Modifier.weight(1f))
            Btn("Megszakítás", onAbort, style = BtnStyle.GHOST)
        }
    }
}

/** "Szép munka!" window after a completed exercise (US-03). */
@Composable
fun SuccessContent(feedback: Feedback, onClose: () -> Unit) {
    val pop = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, tween(350, easing = CubicBezierEasing(0.3f, 1.6f, 0.6f, 1f))) }
    Column(Modifier.fillMaxWidth().background(EC.bg), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.fillMaxWidth().height(150.dp)
                .background(Brush.radialGradient(listOf(EC.primarySoft, Color.Transparent), radius = 320f)),
            contentAlignment = Alignment.Center,
        ) {
            val colors = listOf(EC.primary, EC.ok, EC.accent)
            repeat(14) { i ->
                val a = i / 14.0 * Math.PI * 2
                val r = 60 + (i % 3) * 12
                Box(
                    Modifier.offset(x = (cos(a) * r * 1.4).dp, y = (sin(a) * r * 0.75).dp).size(8.dp).rotate(i * 37f)
                        .alpha(0.55f).clip(RoundedCornerShape(2.dp)).background(colors[i % 3]),
                )
            }
            Box(
                Modifier.size(92.dp).scale(pop.value).alpha(pop.value.coerceIn(0f, 1f)).clip(CircleShape).background(EC.ok),
                contentAlignment = Alignment.Center,
            ) { Icon(Ico.CHECK, 48.dp, EC.okInk, strokeWidth = 2.6f) }
        }
        T("Szép munka!", 25.sp, weight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
        T(
            "Ma ez volt ${feedback.article} ${feedback.doneToday}. szüneted. A szemed hálás lesz érte.",
            13.5.sp, EC.muted, align = TextAlign.Center, lineHeight = 20.sp,
            modifier = Modifier.padding(start = 34.dp, end = 34.dp, top = 8.dp),
        )
        Column(
            Modifier.padding(start = 22.dp, end = 22.dp, top = 18.dp).fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .border(1.dp, EC.line, RoundedCornerShape(10.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                T("Teljesítve ma", 12.5.sp, EC.muted, modifier = Modifier.weight(1f))
                T(if (feedback.total > 0) "${feedback.doneToday} / ${feedback.total}" else "${feedback.doneToday}", 14.sp, weight = FontWeight.Bold)
            }
            Bar(feedback.fraction, Modifier.fillMaxWidth())
        }
        Box(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 20.dp)) {
            Btn("Vissza a munkához", onClose, style = BtnStyle.PRIMARY, icon = Ico.CHECK, large = true, full = true)
        }
    }
}

/** Reminder toast with the two actions of section 3.3 (styled like a native Windows notification). */
@Composable
fun ToastContent(event: ReminderEvent, nowText: String, snoozeMinutes: Int, zone: java.time.ZoneId, onStart: () -> Unit, onSnooze: () -> Unit, onClose: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(EC.toastBg)) {
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Logo(16.dp, 11.dp, 4.dp)
            Spacer(Modifier.width(8.dp))
            T("EyeCare · $nowText", 12.sp, EC.toastText, modifier = Modifier.weight(1f))
            Box(Modifier.size(28.dp, 24.dp).clip(RoundedCornerShape(4.dp)).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
                T("✕", 12.sp, EC.toastText)
            }
        }
        Row(Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(EC.primarySoft), contentAlignment = Alignment.Center) {
                Icon(Ico.EYE, 26.dp, EC.primary)
            }
            Column {
                T("Pihentesd a szemed egy percre", 14.sp, Color.White, FontWeight.SemiBold, Modifier.padding(bottom = 3.dp))
                T(
                    (if (event.isSnoozeRepeat) "Elhalasztott emlékeztető (${Fmt.hm(event.slotAt, zone)}). " else "") +
                        "45 mp-es szemtorna, 3 egyszerű lépés. A munkád mellett is elvégezhető.",
                    13.sp, EC.toastText, lineHeight = 18.sp,
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToastButton("Indítás", primary = true, onClick = onStart, modifier = Modifier.weight(1f))
            ToastButton("Szundi ($snoozeMinutes perc)", primary = false, onClick = onSnooze, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ToastButton(text: String, primary: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier.clip(shape).background(if (primary) EC.primary else EC.toastButton)
            .border(1.dp, if (primary) Color.Transparent else EC.toastButtonBorder, shape)
            .clickable(onClick = onClick).padding(vertical = 7.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) { T(text, 13.sp, if (primary) EC.primaryInk else Color.White, if (primary) FontWeight.SemiBold else FontWeight.Normal) }
}
