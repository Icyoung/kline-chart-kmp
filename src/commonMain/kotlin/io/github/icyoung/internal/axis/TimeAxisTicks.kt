package io.github.icyoung.internal.axis

import kotlin.math.ceil
import kotlin.math.floor

internal data class TimeAxisTick(
    val index: Int,
    val x: Float,
    val textAlpha: Float,
)

internal data class TimeAxisAnchorSet(
    val originTimestamp: Long,
    val originIndex: Int,
    val stepCandles: Int,
)

internal fun calculateTimeAxisAnchorSet(
    candleTimestamps: List<Long>,
    candleWidth: Float,
    candleSpacing: Float,
    xOffset: Float,
    canvasWidth: Float,
    labelCount: Int,
): TimeAxisAnchorSet? {
    val candleCount = candleTimestamps.size
    if (candleCount <= 0 || canvasWidth <= 0f) return null

    val stepPx = candleWidth + candleSpacing
    if (stepPx <= 0f) return null

    val labels = labelCount.coerceAtLeast(2)
    val firstVisibleIndex = ceil((-xOffset - candleWidth / 2f) / stepPx).toInt().coerceAtLeast(0)
    val lastVisibleIndex = floor((canvasWidth - xOffset - candleWidth / 2f) / stepPx).toInt().coerceAtMost(candleCount - 1)
    if (lastVisibleIndex < firstVisibleIndex) return null

    val stepCandles = floor((lastVisibleIndex - firstVisibleIndex).toFloat() / (labels - 1).toFloat())
        .toInt()
        .coerceAtLeast(1)
    return TimeAxisAnchorSet(
        originTimestamp = candleTimestamps[firstVisibleIndex],
        originIndex = firstVisibleIndex,
        stepCandles = stepCandles,
    )
}

internal fun projectTimeAxisTicks(
    anchorSet: TimeAxisAnchorSet?,
    candleTimestamps: List<Long>,
    candleWidth: Float,
    candleSpacing: Float,
    xOffset: Float,
    canvasWidth: Float,
    labelCount: Int,
    fadeAnimation: Boolean,
): List<TimeAxisTick> {
    val candleCount = candleTimestamps.size
    if (anchorSet == null || candleCount <= 0 || canvasWidth <= 0f) return emptyList()

    val stepPx = candleWidth + candleSpacing
    if (stepPx <= 0f) return emptyList()

    val labels = labelCount.coerceAtLeast(2)
    val fadeWidth = (canvasWidth / (labels - 1).toFloat() * 0.45f).coerceAtLeast(stepPx)
    val originIndex = anchorSet.originIndex
    if (originIndex !in 0 until candleCount || candleTimestamps[originIndex] != anchorSet.originTimestamp) return emptyList()

    val firstProjectedIndex = floor((-xOffset - fadeWidth - candleWidth / 2f) / stepPx).toInt()
    val lastProjectedIndex = ceil((canvasWidth + fadeWidth - xOffset - candleWidth / 2f) / stepPx).toInt()
    if (lastProjectedIndex < firstProjectedIndex) return emptyList()

    val firstStep = floor((firstProjectedIndex - originIndex).toDouble() / anchorSet.stepCandles.toDouble()).toInt()
    val lastStep = ceil((lastProjectedIndex - originIndex).toDouble() / anchorSet.stepCandles.toDouble()).toInt()

    return (firstStep..lastStep).mapNotNull { step ->
        val index = originIndex + step * anchorSet.stepCandles
        if (index !in 0 until candleCount) return@mapNotNull null
        val x = index * stepPx + xOffset + candleWidth / 2f
        TimeAxisTick(
            index = index,
            x = x,
            textAlpha = if (fadeAnimation) edgeAlpha(x, canvasWidth, fadeWidth) else 1f,
        )
    }
}

private fun edgeAlpha(x: Float, width: Float, fadeWidth: Float): Float {
    if (fadeWidth <= 0f) return 1f
    val left = (x / fadeWidth).coerceIn(0f, 1f)
    val right = ((width - x) / fadeWidth).coerceIn(0f, 1f)
    return minOf(left, right)
}
