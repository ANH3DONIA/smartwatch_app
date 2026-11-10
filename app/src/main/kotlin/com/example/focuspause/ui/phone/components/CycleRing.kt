/*
 * Copyright 2026 FocusPause
 */
package com.example.focuspause.ui.phone.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.focuspause.data.FocusState
import com.example.focuspause.data.PomodoroMode

/**
 * Anillo segmentado: cada segmento es una sesión de enfoque del ciclo actual.
 * - Segmentos completados: llenos con [doneColor].
 * - Sesión en curso: se llena con [modeColor] según [progress].
 * - Durante una pausa: los segmentos muestran el avance del ciclo y un anillo interior
 *   fino muestra el progreso de la pausa.
 */
@Composable
fun CycleRing(
    state: FocusState,
    progress: Float,
    modeColor: Color,
    doneColor: Color,
    modifier: Modifier = Modifier,
    trackColor: Color = Color(0xFF262626)
) {
    val segments = state.settings.longBreakInterval.coerceAtLeast(1)
    val isFocus = state.mode == PomodoroMode.FOCUS
    val done = when (state.mode) {
        PomodoroMode.FOCUS -> state.cycleCount - 1
        PomodoroMode.SHORT_BREAK -> state.cycleCount
        PomodoroMode.LONG_BREAK -> segments
    }.coerceIn(0, segments)

    Canvas(modifier = modifier) {
        val stroke = 14.dp.toPx()
        val gapDegrees = if (segments > 1) 8f else 0f
        val step = 360f / segments
        val span = step - gapDegrees

        val arcTopLeft = Offset(stroke / 2, stroke / 2)
        val arcSize = Size(size.width - stroke, size.height - stroke)

        for (i in 0 until segments) {
            val start = -90f + i * step + gapDegrees / 2
            drawArc(
                color = trackColor,
                startAngle = start,
                sweepAngle = span,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Butt)
            )
            when {
                i < done -> drawArc(
                    color = doneColor,
                    startAngle = start,
                    sweepAngle = span,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt)
                )
                isFocus && i == done -> drawArc(
                    color = modeColor,
                    startAngle = start,
                    sweepAngle = span * progress.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt)
                )
            }
        }

        if (!isFocus) {
            // Anillo interior fino: progreso de la pausa.
            val thin = 6.dp.toPx()
            val inset = stroke * 2.2f
            val innerTopLeft = Offset(inset, inset)
            val innerSize = Size(size.width - inset * 2, size.height - inset * 2)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = innerTopLeft,
                size = innerSize,
                style = Stroke(width = thin)
            )
            drawArc(
                color = modeColor,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = innerTopLeft,
                size = innerSize,
                style = Stroke(width = thin, cap = StrokeCap.Round)
            )
        }
    }
}
