package io.github.icyoung

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import io.github.icyoung.core.calculateValueY
import io.github.icyoung.model.OhlcvCandle
import kotlin.math.max
import kotlin.math.min

/**
 * Strategy interface for drawing candles in the main chart area.
 *
 * Implement this when the default candlestick appearance is not enough. The
 * renderer receives the visible range and coordinate parameters, but it does
 * not own gestures, axes, overlays, or indicator rendering.
 */
fun interface CandleRenderer {
    fun DrawScope.render(context: CandleRenderContext)
}

/** Rendering context passed to [CandleRenderer]. */
data class CandleRenderContext(
    val candles: List<OhlcvCandle>,
    val visibleRange: IntRange,
    val candleWidth: Float,
    val candleSpacing: Float,
    val xOffset: Float,
    val minPrice: Double,
    val maxPrice: Double,
    val chartHeight: Float,
    val colors: KlineChartColors,
)

object CandleRenderers {
    val Candlestick = CandleRenderer { context ->
        val bodyWidth = context.candleWidth.coerceAtLeast(1f)
        context.visibleRange.forEach { index ->
            if (index !in context.candles.indices) return@forEach

            val candle = context.candles[index]
            val x = index * (context.candleWidth + context.candleSpacing) +
                context.xOffset +
                context.candleWidth / 2
            if (x + bodyWidth < 0 || x - bodyWidth > size.width) return@forEach

            val color = if (candle.close >= candle.open) context.colors.rising else context.colors.falling
            val highY = calculateValueY(candle.high, context.minPrice, context.maxPrice, context.chartHeight)
            val lowY = calculateValueY(candle.low, context.minPrice, context.maxPrice, context.chartHeight)
            val openY = calculateValueY(candle.open, context.minPrice, context.maxPrice, context.chartHeight)
            val closeY = calculateValueY(candle.close, context.minPrice, context.maxPrice, context.chartHeight)

            drawLine(
                color = color,
                start = Offset(x, highY),
                end = Offset(x, lowY),
                strokeWidth = 1.dp.toPx(),
            )
            drawRoundRect(
                color = color,
                topLeft = Offset(x - bodyWidth / 2, min(openY, closeY)),
                size = Size(bodyWidth, max(1f, kotlin.math.abs(closeY - openY))),
                cornerRadius = CornerRadius(1f, 1f),
            )
        }
    }
}
