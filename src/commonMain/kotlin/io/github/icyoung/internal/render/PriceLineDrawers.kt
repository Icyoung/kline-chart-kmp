package io.github.icyoung.internal.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import io.github.icyoung.core.calculateValueY
import io.github.icyoung.model.OhlcvCandle

internal fun DrawScope.drawLatestPriceLine(
    candles: List<OhlcvCandle>,
    candleWidth: Float,
    candleSpacing: Float,
    xOffset: Float,
    price: Double,
    minPrice: Double,
    maxPrice: Double,
    chartWidth: Float,
    color: androidx.compose.ui.graphics.Color,
    candleIndex: Int = candles.lastIndex,
) {
    if (candles.isEmpty() || maxPrice <= minPrice) return
    drawLatestValueLine(
        value = price,
        valueIndex = candleIndex,
        candleWidth = candleWidth,
        candleSpacing = candleSpacing,
        xOffset = xOffset,
        minValue = minPrice,
        maxValue = maxPrice,
        chartWidth = chartWidth,
        color = color,
    )
}

internal fun DrawScope.drawLatestValueLine(
    value: Double,
    valueIndex: Int,
    candleWidth: Float,
    candleSpacing: Float,
    xOffset: Float,
    minValue: Double,
    maxValue: Double,
    chartWidth: Float,
    color: androidx.compose.ui.graphics.Color,
) {
    if (valueIndex < 0 || maxValue <= minValue) return
    val y = calculateValueY(value, minValue, maxValue, size.height)
    if (y < 0f || y > size.height) return
    val lastX = valueIndex * (candleWidth + candleSpacing) + xOffset + candleWidth / 2
    val startX = if (lastX in 0f..chartWidth) lastX else 0f
    drawLine(
        color = color,
        start = Offset(startX, y),
        end = Offset(chartWidth, y),
        strokeWidth = 1.dp.toPx(),
        pathEffect = ChartDashEffect,
    )
}
