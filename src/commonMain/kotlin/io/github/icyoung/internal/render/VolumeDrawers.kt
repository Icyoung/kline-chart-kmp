package io.github.icyoung.internal.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import io.github.icyoung.KlineChartColors
import io.github.icyoung.model.OhlcvCandle

internal fun DrawScope.drawVolumeBars(
    candles: List<OhlcvCandle>,
    candleWidth: Float,
    candleSpacing: Float,
    xOffset: Float,
    maxVolume: Double,
    colors: KlineChartColors,
) {
    val visible = visibleIndexRange(candles.size, candleWidth, candleSpacing, xOffset, size.width)
    visible.forEach { index ->
        val candle = candles[index]
        val x = index * (candleWidth + candleSpacing) + xOffset + candleWidth / 2
        val height = (candle.volume / maxVolume * size.height).toFloat().coerceIn(1f, size.height)
        val color = if (candle.close >= candle.open) {
            colors.rising.copy(alpha = 0.65f)
        } else {
            colors.falling.copy(alpha = 0.65f)
        }
        drawRoundRect(
            color = color,
            topLeft = Offset(x - candleWidth * 0.4f, size.height - height),
            size = Size(candleWidth * 0.8f, height),
            cornerRadius = CornerRadius(1f, 1f),
        )
    }
}

internal fun DrawScope.drawVolumeLine(
    values: List<Double?>,
    candleWidth: Float,
    candleSpacing: Float,
    xOffset: Float,
    maxVolume: Double,
    color: Color,
) {
    val visible = visibleIndexRange(values.size, candleWidth, candleSpacing, xOffset, size.width)
    drawLineIndicator(values, visible, candleWidth, candleSpacing, xOffset, 0.0, maxVolume, size.height, color)
}
