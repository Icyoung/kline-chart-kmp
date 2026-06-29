package io.github.icyoung.internal.render

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import io.github.icyoung.KlineLineStyle
import io.github.icyoung.core.calculateValueY
import io.github.icyoung.model.OhlcvCandle

internal fun DrawScope.drawLineSeries(
    candles: List<OhlcvCandle>,
    visibleRange: IntRange,
    candleWidth: Float,
    candleSpacing: Float,
    xOffset: Float,
    minPrice: Double,
    maxPrice: Double,
    style: KlineLineStyle,
    fallbackColor: Color,
) {
    if (candles.size < 2 || maxPrice <= minPrice) return
    val color = style.color ?: fallbackColor
    val linePath = Path()
    val fillPath = Path()
    var started = false
    var firstX = 0f
    var lastX = 0f

    visibleRange.forEach { index ->
        val candle = candles.getOrNull(index) ?: return@forEach
        val x = index * (candleWidth + candleSpacing) + xOffset + candleWidth / 2
        val y = calculateValueY(candle.close, minPrice, maxPrice, size.height)
        if (!started) {
            linePath.moveTo(x, y)
            fillPath.moveTo(x, y)
            firstX = x
            started = true
        } else {
            linePath.lineTo(x, y)
            fillPath.lineTo(x, y)
        }
        lastX = x
    }

    if (!started) return
    fillPath.lineTo(lastX, size.height)
    fillPath.lineTo(firstX, size.height)
    fillPath.close()
    drawPath(
        path = fillPath,
        brush = Brush.verticalGradient(
            colors = listOf(color.copy(alpha = style.fillAlphaTop), color.copy(alpha = style.fillAlphaBottom)),
            startY = 0f,
            endY = size.height,
        ),
    )
    drawPath(linePath, color, style = Stroke(width = style.lineWidth.toPx()))
}
