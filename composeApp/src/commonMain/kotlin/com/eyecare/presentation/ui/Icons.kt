package com.eyecare.presentation.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** One SVG path. [color] null = current content color; [stroke] null = filled. */
data class SvgPart(
    val d: String,
    val stroke: Float? = 1.8f,
    val color: Color? = null,
    val dash: FloatArray? = null,
)

private fun circle(cx: Float, cy: Float, r: Float) = "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0z"

private fun rect(x: Float, y: Float, w: Float, h: Float, rx: Float): String {
    val iw = w - 2 * rx
    val ih = h - 2 * rx
    return "M${x + rx} ${y}h${iw}a$rx $rx 0 0 1 $rx $rx" +
        "v${ih}a$rx $rx 0 0 1 -$rx $rx" +
        "h${-iw}a$rx $rx 0 0 1 -$rx -$rx" +
        "v${-ih}a$rx $rx 0 0 1 $rx -$rx z"
}

/** Line icons of the demo (24×24 viewBox, 1.8 stroke, round caps). */
enum class Ico(vararg paths: String) {
    EYE("M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12z", circle(12f, 12f, 3f)),
    HOME("M3 11l9-7 9 7v9a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z"),
    CAL(rect(3f, 5f, 18f, 16f, 2f), "M3 10h18M8 3v4M16 3v4"),
    PLAY("M7 5l12 7-12 7z"),
    GEAR(
        circle(12f, 12f, 3f),
        "M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z",
    ),
    CHECK("M5 12.5l4.5 4.5L19 7.5"),
    ZZ("M4 7h6l-6 7h6M14 4h6l-6 7h6"),
    X("M6 6l12 12M18 6L6 18"),
    LOCK(rect(5f, 11f, 14f, 10f, 2f), "M8 11V7a4 4 0 0 1 8 0v4"),
    MOON("M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5z"),
    TRAY("M4 14h4l2 3h4l2-3h4M4 14l2-9h12l2 9v5H4z"),
    BELL("M6 9a6 6 0 0 1 12 0c0 6 2.5 7.5 2.5 7.5h-17S6 15 6 9zM10 20a2 2 0 0 0 4 0"),
    PLUS("M12 5v14M5 12h14"),
    TRASH("M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3"),
    SKIP("M5 5l10 7-10 7zM19 5v14"),
    POWER("M12 3v9M6.3 6.3a8 8 0 1 0 11.4 0"),
    LEFT("M15 5l-7 7 7 7"),
    PANEL(rect(3f, 4f, 18f, 16f, 2f), "M13 20V9h8"),
    ;

    val parts: List<SvgPart> = paths.map { SvgPart(it) }
}

@Composable
fun Icon(icon: Ico, size: Dp = 18.dp, color: Color = LocalContentColor.current, strokeWidth: Float = 1.8f, modifier: Modifier = Modifier) {
    SvgCanvas(24f, 24f, icon.parts.map { it.copy(stroke = strokeWidth) }, modifier.size(size), color)
}

/** Draws SVG path data scaled from a [viewW]×[viewH] viewBox into the modifier's size. */
@Composable
fun SvgCanvas(viewW: Float, viewH: Float, parts: List<SvgPart>, modifier: Modifier, defaultColor: Color = LocalContentColor.current) {
    val parsed = remember(parts) { parts.map { it to PathParser().parsePathString(it.d).toPath() } }
    Canvas(modifier) {
        val s = minOf(size.width / viewW, size.height / viewH)
        val dx = (size.width - viewW * s) / 2
        val dy = (size.height - viewH * s) / 2
        val m = Matrix().apply { translate(dx, dy); scale(s, s) }
        parsed.forEach { (part, raw) ->
            val path = Path().apply { addPath(raw); transform(m) }
            val color = part.color ?: defaultColor
            if (part.stroke == null) {
                drawPath(path, color, style = Fill)
            } else {
                drawPath(
                    path, color,
                    style = Stroke(
                        width = part.stroke * s,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = part.dash?.let { d -> PathEffect.dashPathEffect(d.map { it * s }.toFloatArray()) },
                    ),
                )
            }
        }
    }
}

/** Exercise step illustrations from the demo (96×58 viewBox). */
object StepArt {
    fun parts(key: String): List<SvgPart> = when (key) {
        "far" -> listOf(
            SvgPart("M6 50 L34 18 L50 34 L62 22 L90 50", stroke = 3.5f, color = EC.text),
            SvgPart(circle(74f, 12f, 6f), stroke = 3f, color = EC.primary),
        )
        "closed" -> listOf(
            SvgPart("M10 26 Q48 52 86 26", stroke = 4f, color = EC.text),
            SvgPart("M22 36l-6 8M36 42l-3 9M60 42l3 9M74 36l6 8", stroke = 3f, color = EC.text),
        )
        else -> listOf(
            SvgPart("M8 30 Q48 2 88 30 Q48 46 8 30z", stroke = 3.5f, color = EC.text),
            SvgPart("M8 30 Q48 18 88 30", stroke = 3f, color = EC.text, dash = floatArrayOf(4f, 5f)),
            SvgPart(circle(48f, 31f, 7f), stroke = null, color = EC.text),
        )
    }
}
