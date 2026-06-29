package io.github.icyoung.internal.render

import io.github.icyoung.util.formatChartValue
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal fun visibleIndexRange(
    dataSize: Int,
    candleWidth: Float,
    candleSpacing: Float,
    xOffset: Float,
    canvasWidth: Float,
): IntRange {
    if (dataSize <= 0) return IntRange.EMPTY
    val step = candleWidth + candleSpacing
    val startIndex = max(0, ((-xOffset - candleWidth) / step).toInt() - 1)
    val endIndex = min(dataSize - 1, ((canvasWidth - xOffset) / step).toInt() + 1)
    return if (startIndex > endIndex) IntRange.EMPTY else startIndex..endIndex
}

internal fun <T> visibleMax(
    items: List<T>,
    candleWidth: Float,
    candleSpacing: Float,
    xOffset: Float,
    canvasWidth: Float,
    value: (T) -> Double,
): Double {
    val range = visibleIndexRange(items.size, candleWidth, candleSpacing, xOffset, canvasWidth)
    if (range.isEmpty()) return items.firstOrNull()?.let(value) ?: 1.0
    return range.maxOfOrNull { index -> value(items[index]) } ?: 1.0
}

internal fun visibleMinMax(
    candleWidth: Float,
    candleSpacing: Float,
    xOffset: Float,
    canvasWidth: Float,
    vararg lists: List<Double?>,
): Pair<Double, Double> {
    var minValue = Double.MAX_VALUE
    var maxValue = -Double.MAX_VALUE
    val maxSize = lists.maxOfOrNull { it.size } ?: 0
    val range = visibleIndexRange(maxSize, candleWidth, candleSpacing, xOffset, canvasWidth)
    for (list in lists) {
        for (index in range) {
            val value = list.getOrNull(index) ?: continue
            if (value < minValue) minValue = value
            if (value > maxValue) maxValue = value
        }
    }
    if (minValue > maxValue) return 0.0 to 0.0
    val padding = (maxValue - minValue).let {
        if (it > 0) it * 0.1 else abs(maxValue) * 0.1 + 1e-9
    }
    return (minValue - padding) to (maxValue + padding)
}

internal fun Double.formatCompact(): String {
    return when {
        abs(this) >= 1_000_000_000 -> "${(this / 1_000_000_000).formatChartValue(2)}B"
        abs(this) >= 1_000_000 -> "${(this / 1_000_000).formatChartValue(2)}M"
        abs(this) >= 1_000 -> "${(this / 1_000).formatChartValue(2)}K"
        else -> formatChartValue(2)
    }
}
