package io.github.icyoung.internal.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.github.icyoung.core.calculateValueY

internal fun DrawScope.drawLineIndicator(
    values: List<Double?>,
    visibleRange: IntRange,
    candleWidth: Float,
    candleSpacing: Float,
    xOffset: Float,
    minValue: Double,
    maxValue: Double,
    chartHeight: Float,
    color: Color,
) {
    if (maxValue <= minValue) return
    val path = Path()
    var started = false
    visibleRange.forEach { index ->
        val value = values.getOrNull(index) ?: run {
            started = false
            return@forEach
        }
        val x = index * (candleWidth + candleSpacing) + xOffset + candleWidth / 2
        val y = calculateValueY(value, minValue, maxValue, chartHeight)
        if (!started) {
            path.moveTo(x, y)
            started = true
        } else {
            path.lineTo(x, y)
        }
    }
    drawPath(path, color, style = Stroke(width = 1.dp.toPx()))
}

internal fun DrawScope.drawSar(
    values: List<Double?>,
    visibleRange: IntRange,
    candleWidth: Float,
    candleSpacing: Float,
    xOffset: Float,
    minValue: Double,
    maxValue: Double,
    chartHeight: Float,
    color: Color,
) {
    visibleRange.forEach { index ->
        val value = values.getOrNull(index) ?: return@forEach
        val x = index * (candleWidth + candleSpacing) + xOffset + candleWidth / 2
        val y = calculateValueY(value, minValue, maxValue, chartHeight)
        drawCircle(color, radius = 1.5.dp.toPx(), center = Offset(x, y))
    }
}
