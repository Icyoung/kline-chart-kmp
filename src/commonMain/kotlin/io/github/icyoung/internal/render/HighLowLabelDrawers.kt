package io.github.icyoung.internal.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import io.github.icyoung.core.calculateValueY
import io.github.icyoung.model.OhlcvCandle
import io.github.icyoung.util.formatChartValue

@OptIn(ExperimentalTextApi::class)
internal fun DrawScope.drawHighLowLabels(
    candles: List<OhlcvCandle>,
    visibleRange: IntRange,
    candleWidth: Float,
    candleSpacing: Float,
    xOffset: Float,
    minPrice: Double,
    maxPrice: Double,
    chartWidth: Float,
    precision: Int,
    color: Color,
    textMeasurer: TextMeasurer,
    style: TextStyle,
) {
    if (candles.isEmpty() || visibleRange.isEmpty() || maxPrice <= minPrice) return
    var highest = -Double.MAX_VALUE
    var lowest = Double.MAX_VALUE
    var highestIndex = -1
    var lowestIndex = -1
    val step = candleWidth + candleSpacing
    visibleRange.forEach { index ->
        val candle = candles.getOrNull(index) ?: return@forEach
        val x = index * step + xOffset + candleWidth / 2
        if (x < -candleWidth || x > chartWidth + candleWidth) return@forEach
        if (candle.high > highest) {
            highest = candle.high
            highestIndex = index
        }
        if (candle.low < lowest) {
            lowest = candle.low
            lowestIndex = index
        }
    }
    drawPriceMarkerLabel(highestIndex, highest, candleWidth, candleSpacing, xOffset, minPrice, maxPrice, chartWidth, precision, color, textMeasurer, style)
    drawPriceMarkerLabel(lowestIndex, lowest, candleWidth, candleSpacing, xOffset, minPrice, maxPrice, chartWidth, precision, color, textMeasurer, style)
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawPriceMarkerLabel(
    candleIndex: Int,
    price: Double,
    candleWidth: Float,
    candleSpacing: Float,
    xOffset: Float,
    minPrice: Double,
    maxPrice: Double,
    chartWidth: Float,
    precision: Int,
    color: Color,
    textMeasurer: TextMeasurer,
    style: TextStyle,
) {
    if (candleIndex < 0) return
    val x = candleIndex * (candleWidth + candleSpacing) + xOffset + candleWidth / 2
    if (x < 0f || x > chartWidth) return
    val y = calculateValueY(price, minPrice, maxPrice, size.height)
    val lineLength = 50f
    val textPadding = 8f
    val onLeft = x < chartWidth / 2
    val lineStartX = if (onLeft) x else x - lineLength
    val lineEndX = if (onLeft) x + lineLength else x
    drawLine(color, Offset(lineStartX, y), Offset(lineEndX, y), strokeWidth = 1f)
    val text = price.formatChartValue(precision)
    val layout = textMeasurer.measure(text, style.copy(color = color))
    val textX = if (onLeft) lineEndX + textPadding else lineStartX - layout.size.width - textPadding
    val maxTextX = (chartWidth - layout.size.width.toFloat()).coerceAtLeast(0f)
    val maxTextY = (size.height - layout.size.height.toFloat()).coerceAtLeast(0f)
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(
            x = textX.coerceIn(0f, maxTextX),
            y = (y - layout.size.height / 2).coerceIn(0f, maxTextY),
        ),
    )
}
