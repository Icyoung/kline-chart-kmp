package io.github.icyoung.internal.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.github.icyoung.KlineChartColors
import io.github.icyoung.core.calculateValueY
import io.github.icyoung.indicator.TechnicalIndicators
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

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

internal fun DrawScope.drawMacd(
    macd: TechnicalIndicators.MACD,
    candleWidth: Float,
    candleSpacing: Float,
    xOffset: Float,
    minValue: Double,
    maxValue: Double,
    colors: KlineChartColors,
) {
    if (maxValue <= minValue) return
    val zeroY = calculateValueY(0.0, minValue, maxValue, size.height)
    val visible = visibleIndexRange(macd.histogram.size, candleWidth, candleSpacing, xOffset, size.width)
    visible.forEach { index ->
        val hist = macd.histogram.getOrNull(index) ?: return@forEach
        val x = index * (candleWidth + candleSpacing) + xOffset + candleWidth / 2
        val y = calculateValueY(hist, minValue, maxValue, size.height)
        val color = if (hist >= 0) colors.rising else colors.falling
        drawRoundRect(
            color = color,
            topLeft = Offset(x - candleWidth * 0.35f, min(y, zeroY)),
            size = Size(candleWidth * 0.7f, max(1f, abs(zeroY - y))),
            cornerRadius = CornerRadius(1f, 1f),
        )
    }
    drawLineIndicator(macd.macdLine, visible, candleWidth, candleSpacing, xOffset, minValue, maxValue, size.height, colors.indicator1)
    drawLineIndicator(macd.signalLine, visible, candleWidth, candleSpacing, xOffset, minValue, maxValue, size.height, colors.indicator2)
}
