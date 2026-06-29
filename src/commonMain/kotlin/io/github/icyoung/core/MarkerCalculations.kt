package io.github.icyoung.core

import io.github.icyoung.KlineHistoryMarker
import io.github.icyoung.KlineMarkerAggregation
import io.github.icyoung.KlineMarkerCollision
import io.github.icyoung.KlineMarkerPlacement
import io.github.icyoung.KlineMarkerStyle
import io.github.icyoung.model.OhlcvCandle
import kotlin.math.abs

fun findCandleIndexForTimestamp(candles: List<OhlcvCandle>, timestamp: Long): Int {
    var left = 0
    var right = candles.lastIndex
    while (left <= right) {
        val mid = (left + right) / 2
        val candle = candles[mid]
        when {
            timestamp < candle.timestamp -> right = mid - 1
            timestamp > candle.endTimestamp -> left = mid + 1
            else -> return mid
        }
    }
    return -1
}

fun markersForCandle(candle: OhlcvCandle, markers: List<KlineHistoryMarker>): List<KlineHistoryMarker> {
    return markers.filter { it.timestamp in candle.timestamp..candle.endTimestamp }
}

data class KlineMarkerHitTestConfig(
    val markerSizePx: Float = 28f,
    val verticalOffsetPx: Float = 30f,
    val hitSlopPx: Float = 8f,
)

fun hitTestHistoryMarker(
    markers: List<KlineHistoryMarker>,
    candles: List<OhlcvCandle>,
    candleWidth: Float,
    candleSpacing: Float,
    xOffset: Float,
    minPrice: Double,
    maxPrice: Double,
    canvasHeight: Float,
    tapX: Float,
    tapY: Float,
    config: KlineMarkerHitTestConfig = KlineMarkerHitTestConfig(),
): KlineHistoryMarker? {
    if (markers.isEmpty() || candles.isEmpty() || maxPrice <= minPrice || canvasHeight <= 0f) return null
    val step = candleWidth + candleSpacing
    val hitHalfSize = config.markerSizePx / 2f + config.hitSlopPx

    return markers.firstOrNull { marker ->
        val index = findCandleIndexForTimestamp(candles, marker.timestamp)
        if (index < 0) return@firstOrNull false
        val candle = candles[index]
        val centerX = index * step + xOffset + candleWidth / 2f
        val anchorPrice = if (marker.placement == KlineMarkerPlacement.Below) candle.low else candle.high
        val anchorY = valueToY(anchorPrice, minPrice, maxPrice, canvasHeight)
        val centerY = if (marker.placement == KlineMarkerPlacement.Below) {
            anchorY + config.verticalOffsetPx
        } else {
            anchorY - config.verticalOffsetPx
        }
        abs(tapX - centerX) <= hitHalfSize && abs(tapY - centerY) <= hitHalfSize
    }
}

data class KlineMarkerLayout(
    val markers: List<KlineHistoryMarker>,
    val candleIndex: Int,
    val placement: KlineMarkerPlacement,
    val label: String,
    val centerX: Float,
    val centerY: Float,
)

fun calculateMarkerLayouts(
    markers: List<KlineHistoryMarker>,
    candles: List<OhlcvCandle>,
    candleWidth: Float,
    candleSpacing: Float,
    xOffset: Float,
    minPrice: Double,
    maxPrice: Double,
    canvasHeight: Float,
    style: KlineMarkerStyle = KlineMarkerStyle(),
): List<KlineMarkerLayout> {
    if (markers.isEmpty() || candles.isEmpty() || maxPrice <= minPrice || canvasHeight <= 0f) return emptyList()
    val grouped = when (style.aggregation) {
        KlineMarkerAggregation.None -> markers.map { listOf(it) }
        KlineMarkerAggregation.ByCandleAndSide -> markers
            .groupBy { marker -> findCandleIndexForTimestamp(candles, marker.timestamp) to marker.placement }
            .values
            .filter { group -> findCandleIndexForTimestamp(candles, group.first().timestamp) >= 0 }
    }

    val placementCounts = mutableMapOf<Pair<Int, KlineMarkerPlacement>, Int>()
    return grouped.mapNotNull { group ->
        val first = group.firstOrNull() ?: return@mapNotNull null
        val index = findCandleIndexForTimestamp(candles, first.timestamp)
        if (index < 0) return@mapNotNull null
        val candle = candles[index]
        val placement = first.placement
        val key = index to placement
        val stackIndex = placementCounts.getOrElse(key) { 0 }
        placementCounts[key] = stackIndex + 1
        val stackOffset = if (style.collision == KlineMarkerCollision.Stack) {
            stackIndex * style.stackSpacingPx
        } else {
            0f
        }
        val anchorPrice = if (placement == KlineMarkerPlacement.Below) candle.low else candle.high
        val anchorY = valueToY(anchorPrice, minPrice, maxPrice, canvasHeight)
        val direction = if (placement == KlineMarkerPlacement.Below) 1f else -1f
        KlineMarkerLayout(
            markers = group,
            candleIndex = index,
            placement = placement,
            label = markerGroupLabel(group),
            centerX = indexToX(index, candleWidth, candleSpacing, xOffset),
            centerY = anchorY + direction * (style.verticalOffsetPx + stackOffset),
        )
    }
}

private fun markerGroupLabel(markers: List<KlineHistoryMarker>): String {
    if (markers.size == 1) return markers.first().label
    val first = markers.first().label
    return "$first x${markers.size}"
}

private fun valueToY(value: Double, minValue: Double, maxValue: Double, height: Float): Float {
    if (maxValue <= minValue) return height / 2f
    return (height - ((value - minValue) / (maxValue - minValue) * height)).toFloat()
}
