package com.eyecare.presentation.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.eyecare.core.advertisement.AdBanner

// ---------- text ----------

val Tabular = "tnum"

@Composable
fun T(
    text: String,
    size: TextUnit = 13.5.sp,
    color: Color = LocalContentColor.current,
    weight: FontWeight = FontWeight.Normal,
    modifier: Modifier = Modifier,
    align: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    tabular: Boolean = false,
    letterSpacing: TextUnit = TextUnit.Unspecified,
) {
    Text(
        text,
        modifier = modifier,
        style = TextStyle(
            fontSize = size,
            color = color,
            fontWeight = weight,
            lineHeight = lineHeight,
            letterSpacing = letterSpacing,
            fontFeatureSettings = if (tabular) Tabular else null,
            textAlign = align ?: TextAlign.Unspecified,
        ),
    )
}

/** Page heading: h2 + muted sub line. */
@Composable
fun Heading(title: String, sub: String? = null, modifier: Modifier = Modifier) {
    Column(modifier.padding(bottom = 14.dp)) {
        T(title, 20.sp, weight = FontWeight.SemiBold, letterSpacing = (-0.01).em)
        if (sub != null) T(sub, 12.5.sp, EC.muted, modifier = Modifier.padding(top = 3.dp), lineHeight = 17.sp)
    }
}

// ---------- surfaces ----------

@Composable
fun Card(modifier: Modifier = Modifier, brush: Brush? = null, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (brush != null) Modifier.background(brush) else Modifier.background(EC.surface))
            .border(1.dp, EC.line, shape)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        content = content,
    )
}

/** Uppercase card title with icon. */
@Composable
fun CardTitle(text: String, icon: Ico? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
        if (icon != null) {
            Icon(icon, 18.dp, EC.muted)
            Spacer(Modifier.width(8.dp))
        }
        T(text.uppercase(), 12.sp, EC.muted, FontWeight.SemiBold, letterSpacing = 0.06.em)
    }
}

// ---------- buttons ----------

enum class BtnStyle { PRIMARY, SECONDARY, GHOST, DANGER }

@Composable
fun Btn(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: BtnStyle = BtnStyle.SECONDARY,
    icon: Ico? = null,
    large: Boolean = false,
    full: Boolean = false,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(999.dp)
    val (bg, fg, border) = when (style) {
        BtnStyle.PRIMARY -> Triple(EC.primary, EC.primaryInk, Color.Transparent)
        BtnStyle.SECONDARY -> Triple(EC.surface, EC.text, EC.line)
        BtnStyle.GHOST -> Triple(Color.Transparent, EC.muted, Color.Transparent)
        BtnStyle.DANGER -> Triple(Color.Transparent, EC.accent, Color.Transparent)
    }
    val padding = when {
        style == BtnStyle.DANGER -> PaddingValues(start = 0.dp, end = 16.dp, top = 10.dp, bottom = 10.dp)
        large -> PaddingValues(horizontal = 20.dp, vertical = 13.dp)
        else -> PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    }
    Row(
        modifier
            .then(if (full) Modifier.fillMaxWidth() else Modifier)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(padding),
        horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, 18.dp, fg)
        T(text, if (large) 14.5.sp else 13.5.sp, fg, FontWeight.SemiBold)
    }
}

@Composable
fun Chip(text: String, icon: Ico? = null) {
    Row(
        Modifier.clip(RoundedCornerShape(999.dp)).background(EC.primarySoft).padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) Icon(icon, 12.dp, EC.primary)
        T(text, 11.5.sp, EC.primary, FontWeight.Bold)
    }
}

// ---------- ring ----------

@Composable
fun Ring(size: Dp, fraction: Float, stroke: Float, color: Color = EC.primary, content: @Composable () -> Unit) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val s = this.size.width / 100f
            val sw = stroke * s
            val r = 44f * s
            val tl = Offset(center.x - r, center.y - r)
            val arcSize = androidx.compose.ui.geometry.Size(2 * r, 2 * r)
            drawArc(EC.surface2, 0f, 360f, false, tl, arcSize, style = Stroke(sw))
            val f = fraction.coerceIn(0f, 1f)
            if (f > 0f) drawArc(color, -90f, 360f * f, false, tl, arcSize, style = Stroke(sw, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) { content() }
    }
}

// ---------- progress bar ----------

@Composable
fun Bar(fraction: Float, modifier: Modifier = Modifier) {
    Box(modifier.height(7.dp).clip(RoundedCornerShape(9.dp)).background(EC.surface2)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0f, 1f)).clip(RoundedCornerShape(9.dp)).background(EC.ok))
    }
}

// ---------- form controls ----------

@Composable
fun FieldLabel(text: String) = T(text, 12.sp, EC.muted, FontWeight.SemiBold, Modifier.padding(bottom = 7.dp))

@Composable
fun Field(label: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(bottom = 14.dp)) {
        FieldLabel(label)
        content()
    }
}

/** Segmented control (interval). */
@Composable
fun <T> Segmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(999.dp)).background(EC.surface2).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { (value, label) ->
            val on = value == selected
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(999.dp))
                    .background(if (on) EC.surface else Color.Transparent)
                    .clickable { onSelect(value) }
                    .padding(vertical = 7.dp, horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) { T(label, 12.5.sp, if (on) EC.primary else EC.muted, FontWeight.SemiBold) }
        }
    }
}

/** Rounded choice buttons (snooze length). */
@Composable
fun <T> Choice(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            val on = value == selected
            val shape = RoundedCornerShape(999.dp)
            Box(
                Modifier.weight(1f).clip(shape)
                    .background(if (on) EC.primarySoft else Color.Transparent)
                    .border(1.dp, if (on) EC.primary else EC.line, shape)
                    .clickable { onSelect(value) }
                    .padding(vertical = 8.dp, horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) { T(label, 12.5.sp, if (on) EC.primary else EC.muted, FontWeight.SemiBold) }
        }
    }
}

/** Day-of-week picker (H K Sze Cs P Szo V). */
@Composable
fun DayPick(labels: List<String>, selected: (Int) -> Boolean, onToggle: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        labels.forEachIndexed { i, label ->
            val on = selected(i)
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                    .background(if (on) EC.primary else EC.surface2)
                    .clickable { onToggle(i) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) { T(label, 12.sp, if (on) EC.primaryInk else EC.muted, FontWeight.Bold) }
        }
    }
}

@Composable
fun Switch(on: Boolean, onToggle: () -> Unit) {
    Box(
        Modifier.size(40.dp, 22.dp).clip(RoundedCornerShape(99.dp)).background(if (on) EC.primary else EC.miss).clickable(onClick = onToggle),
    ) {
        Box(
            Modifier.offset(x = if (on) 21.dp else 3.dp, y = 3.dp).size(16.dp).clip(CircleShape)
                .background(if (on) EC.primaryInk else Color.White),
        )
    }
}

/** Time select in 30-minute steps, styled like the demo's <select>. */
@Composable
fun TimeSelect(caption: String, value: Int, onChange: (Int) -> Unit, note: String, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Column(modifier) {
        T(caption, 11.sp, EC.muted, modifier = Modifier.padding(bottom = 4.dp))
        Box {
            val shape = RoundedCornerShape(10.dp)
            Row(
                Modifier.fillMaxWidth().clip(shape).background(EC.surface2).border(1.dp, EC.line, shape)
                    .clickable { open = true }.padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                T(com.eyecare.presentation.Fmt.minutes(value), 15.sp, weight = FontWeight.SemiBold, tabular = true, modifier = Modifier.weight(1f))
                T("▾", 12.sp, EC.muted)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = EC.surface2, modifier = Modifier.height(260.dp)) {
                (0 until 48).map { it * 30 }.forEach { m ->
                    DropdownMenuItem(
                        text = { T(com.eyecare.presentation.Fmt.minutes(m), 14.sp, if (m == value) EC.primary else EC.text, FontWeight.SemiBold, tabular = true) },
                        onClick = { onChange(m); open = false },
                    )
                }
            }
        }
        T(note, 11.5.sp, EC.primary, FontWeight.SemiBold, Modifier.height(17.dp).padding(top = 3.dp))
    }
}

/** Highlighted summary box (bell + text). */
@Composable
fun Summary(text: String, bad: Boolean) {
    val color = if (bad) EC.accent else EC.primary
    Row(
        Modifier.fillMaxWidth().padding(top = 6.dp).clip(RoundedCornerShape(10.dp)).background(EC.primarySoft).padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Ico.BELL, 18.dp, color)
        T(text, 12.5.sp, color, lineHeight = 18.sp)
    }
}

/** Icon + text info line (settings). */
@Composable
fun Info(icon: Ico, bold: String, rest: String) {
    Row(Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, 18.dp, EC.primary, modifier = Modifier.padding(top = 1.dp))
        Text(
            androidx.compose.ui.text.buildAnnotatedString {
                pushStyle(androidx.compose.ui.text.SpanStyle(color = EC.text, fontWeight = FontWeight.SemiBold)); append(bold); pop()
                append(rest)
            },
            style = TextStyle(fontSize = 12.5.sp, color = EC.muted, lineHeight = 18.sp),
        )
    }
}

// ---------- ad strip ----------

/** Static banner strip shown above the panel navigation (US-18). */
@Composable
fun AdStrip(banner: AdBanner, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(10.dp)).background(EC.surface2)
            .drawBehind {
                drawRoundRect(
                    EC.line, cornerRadius = CornerRadius(10.dp.toPx()),
                    style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
                )
            },
    ) {
        Row(Modifier.fillMaxHeight().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(76.dp, 36.dp).clip(RoundedCornerShape(7.dp))
                    .background(Brush.linearGradient(listOf(Color(banner.from), Color(banner.to)))),
                contentAlignment = Alignment.Center,
            ) { T(banner.brand, 11.5.sp, Color.White, FontWeight.ExtraBold) }
            Column {
                T(banner.title, 12.5.sp, weight = FontWeight.Bold)
                T(banner.subtitle, 11.5.sp, EC.muted)
            }
        }
        T("HIRDETÉS", 9.5.sp, EC.muted, letterSpacing = 0.06.em, modifier = Modifier.align(Alignment.TopEnd).padding(top = 5.dp, end = 9.dp))
    }
}

/** Small orange logo tile used in title bars and the toast. */
@Composable
fun Logo(size: Dp = 20.dp, iconSize: Dp = 13.dp, radius: Dp = 6.dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(radius)).background(EC.primary), contentAlignment = Alignment.Center) {
        Icon(Ico.EYE, iconSize, EC.primaryInk)
    }
}

@Composable
fun RowScope.Grow() = Spacer(Modifier.weight(1f))

@Composable
fun ProvideMuted(content: @Composable () -> Unit) = CompositionLocalProvider(LocalContentColor provides EC.muted, content = content)
