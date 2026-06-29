package io.github.icyoung.core

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

data class VisibleRange(
    val indices: IntRange,
    val minValue: Double,
    val maxValue: Double,
)

fun <T> calculateVisibleRange(
    candles: List<T>,
    candleWidth: Float,
    candleSpacing: Float,
    xOffset: Float,
    canvasWidth: Float,
    low: (T) -> Double,
    high: (T) -> Double,
    close: (T) -> Double,
): VisibleRange {
    if (candles.isEmpty()) return VisibleRange(0..0, 0.0, 100.0)

    val candleStepWidth = candleWidth + candleSpacing
    val rawStartIndex = (-xOffset - candleWidth) / candleStepWidth
    val startIndex = max(0, floor(rawStartIndex).toInt() - 2)
    val rawEndIndex = (canvasWidth - xOffset) / candleStepWidth
    val endIndex = min(candles.size - 1, ceil(rawEndIndex).toInt() + 2)

    if (startIndex > endIndex) {
        val firstClose = close(candles.first())
        return VisibleRange(0..0, firstClose, firstClose)
    }

    val visibleCandles = candles.subList(
        max(0, startIndex),
        min(candles.size, endIndex + 1),
    )
    val minPrice = visibleCandles.minOfOrNull(low) ?: 0.0
    val maxPrice = visibleCandles.maxOfOrNull(high) ?: 100.0
    val priceRange = maxPrice - minPrice
    val padding = if (priceRange > 0) priceRange * 0.1 else maxPrice * 0.05

    return VisibleRange(
        indices = startIndex..endIndex,
        minValue = minPrice - padding,
        maxValue = maxPrice + padding,
    )
}

fun calculateScrollBounds(
    itemCount: Int,
    candleWidth: Float,
    candleSpacing: Float,
    canvasWidth: Float,
): Pair<Float, Float> {
    if (itemCount <= 0) return 0f to 0f

    val candleStepWidth = candleWidth + candleSpacing
    val totalChartWidth = itemCount * candleStepWidth

    if (totalChartWidth <= canvasWidth) {
        val centerOffset = (canvasWidth - totalChartWidth) / 2f
        return centerOffset to centerOffset
    }

    val edgeMargin = canvasWidth / 4f
    val leftBound = canvasWidth - edgeMargin - (itemCount - 1) * candleStepWidth - candleWidth / 2
    val rightBound = edgeMargin - candleWidth / 2
    return leftBound to rightBound
}

fun calculateValueY(
    value: Double,
    minValue: Double,
    maxValue: Double,
    canvasHeight: Float,
): Float {
    val valueRange = maxValue - minValue
    if (valueRange <= 0) return canvasHeight / 2f

    val valueRatio = (value - minValue) / valueRange
    return canvasHeight - (valueRatio * canvasHeight).toFloat()
}
