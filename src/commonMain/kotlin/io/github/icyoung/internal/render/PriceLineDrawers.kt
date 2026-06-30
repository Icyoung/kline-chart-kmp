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
    val y = calculateValueY(price, minPrice, maxPrice, size.height)
    if (y < 0f || y > size.height) return
    val lastX = candleIndex * (candleWidth + candleSpacing) + xOffset + candleWidth / 2
    val startX = if (lastX in 0f..chartWidth) lastX else 0f
    drawLine(
        color = color,
        start = Offset(startX, y),
        end = Offset(chartWidth, y),
        strokeWidth = 1.dp.toPx(),
        pathEffect = ChartDashEffect,
    )
}
