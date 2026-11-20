package com.eyecare.platform.window

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.eyecare.presentation.ui.EC
import com.eyecare.presentation.ui.EyeCareTheme
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

/** Usable screen area (excludes taskbar / Dock / menu bar). */
fun usableBounds(): Rectangle = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds

private const val MARGIN_RIGHT = 12
private const val MARGIN_BOTTOM = 10

/**
 * Undecorated, rounded window anchored to the bottom-right corner of the screen, like the demo's
 * tray flyout, floating exercise window and toast. With [height] = null the window is packed to its
 * content and re-anchored whenever its size changes.
 */
@Composable
fun CornerWindow(
    title: String,
    width: Dp,
    height: Dp?,
    icon: Painter?,
    alwaysOnTop: Boolean,
    focusable: Boolean = true,
    radius: Dp = 12.dp,
    background: Color = EC.bg,
    border: Color = EC.winBorder,
    onCloseRequest: () -> Unit,
    onFocusLost: (() -> Unit)? = null,
    visible: Boolean = true,
    content: @Composable FrameWindowScope.() -> Unit,
) {
    val bounds = remember { usableBounds() }
    val estimatedHeight = height?.value?.toInt() ?: 400
    val state = rememberWindowState(
        size = DpSize(width, height ?: Dp.Unspecified),
        position = WindowPosition(
            (bounds.x + bounds.width - width.value.toInt() - MARGIN_RIGHT).dp,
            (bounds.y + bounds.height - estimatedHeight - MARGIN_BOTTOM).dp,
        ),
    )
    Window(
        onCloseRequest = onCloseRequest,
        state = state,
        visible = visible,
        title = title,
        icon = icon,
        undecorated = true,
        transparent = true,
        resizable = false,
        focusable = focusable,
        alwaysOnTop = alwaysOnTop,
    ) {
        DisposableEffect(window) {
            val w = window
            fun anchor() {
                val b = usableBounds()
                w.setLocation(b.x + b.width - w.width - MARGIN_RIGHT, b.y + b.height - w.height - MARGIN_BOTTOM)
            }
            val resize = object : ComponentAdapter() {
                override fun componentResized(e: ComponentEvent?) = anchor()
            }
            val focus = object : WindowAdapter() {
                override fun windowLostFocus(e: WindowEvent?) {
                    onFocusLost?.invoke()
                }
            }
            if (height == null) {
                anchor()
                w.addComponentListener(resize)
            }
            w.addWindowFocusListener(focus)
            onDispose {
                w.removeComponentListener(resize)
                w.removeWindowFocusListener(focus)
            }
        }
        EyeCareTheme {
            val shape = RoundedCornerShape(radius)
            val frame = Modifier.clip(shape).background(background).then(Modifier.border(1.dp, border, shape))
            Box(if (height != null) frame.fillMaxSize() else frame.width(width)) { content() }
        }
    }
}
