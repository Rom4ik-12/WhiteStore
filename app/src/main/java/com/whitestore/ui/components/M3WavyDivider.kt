package com.whitestore.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * Material 3 Expressive wavy divider.
 * Creates an organic, playful decorative wave line between settings sections.
 */
@Composable
fun M3WavyDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
    waveHeight: Dp = 4.dp,
    waveLength: Dp = 16.dp,
    strokeWidth: Dp = 1.5.dp
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(waveHeight * 2 + strokeWidth)
            .padding(horizontal = 4.dp)
    ) {
        val width = size.width
        val centerY = size.height / 2f
        val amplitude = waveHeight.toPx()
        val wavelengthPx = waveLength.toPx()

        if (wavelengthPx <= 0f) {
            drawLine(
                color = color,
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = strokeWidth.toPx(),
                cap = StrokeCap.Round
            )
            return@Canvas
        }

        val path = Path()
        path.moveTo(0f, centerY)

        var x = 0f
        val step = 2f
        while (x <= width) {
            val radians = (x / wavelengthPx) * (2f * Math.PI.toFloat())
            val y = centerY + amplitude * sin(radians)
            path.lineTo(x, y)
            x += step
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        )
    }
}
