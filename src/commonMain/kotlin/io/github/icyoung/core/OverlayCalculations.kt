package io.github.icyoung.core

import io.github.icyoung.KlineOverlayLine
import io.github.icyoung.KlineOverlayLineHit
import kotlin.math.abs

fun priceToY(price: Double, minPrice: Double, maxPrice: Double, canvasHeight: Float): Float {
    return calculateValueY(price, minPrice, maxPrice, canvasHeight)
}

fun yToPrice(y: Float, minPrice: Double, maxPrice: Double, canvasHeight: Float): Double {
    if (canvasHeight <= 0f) return minPrice
    return maxPrice - (y / canvasHeight) * (maxPrice - minPrice)
}

fun indexToX(index: Int, candleWidth: Float, candleSpacing: Float, xOffset: Float): Float {
    return index * (candleWidth + candleSpacing) + xOffset + candleWidth / 2f
}

fun timestampToX(
    candleIndex: Int,
    candleWidth: Float,
    candleSpacing: Float,
    xOffset: Float,
): Float {
    return indexToX(candleIndex, candleWidth, candleSpacing, xOffset)
}

fun hitTestOverlayLine(
    lines: List<KlineOverlayLine>,
    minPrice: Double,
    maxPrice: Double,
    canvasHeight: Float,
    tapY: Float,
    hitSlopPx: Float = 10f,
): KlineOverlayLineHit? {
    if (maxPrice <= minPrice || canvasHeight <= 0f) return null
    return lines.firstOrNull { line ->
        abs(priceToY(line.price, minPrice, maxPrice, canvasHeight) - tapY) <= hitSlopPx
    }?.let { KlineOverlayLineHit(it.id, it) }
}
