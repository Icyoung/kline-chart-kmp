package io.github.icyoung.internal.axis

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.icyoung.KlineChartColors
import io.github.icyoung.KlineChartConfig
import io.github.icyoung.core.calculateValueY
import io.github.icyoung.model.OhlcvCandle
import io.github.icyoung.util.formatChartValue
import kotlin.math.max
import kotlin.math.min

@Composable
internal fun TimeAxis(
    candles: List<OhlcvCandle>,
    config: KlineChartConfig,
    candleWidth: Float,
    candleSpacing: Float,
    xOffset: Float,
    colors: KlineChartColors,
    modifier: Modifier,
) {
    BoxWithConstraints(modifier.background(colors.background)) {
        val axisWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
        Canvas(Modifier.fillMaxSize()) {
            if (candles.isEmpty()) return@Canvas
            val step = candleWidth + candleSpacing
            val startIndex = max(0, ((-xOffset) / step).toInt())
            val endIndex = min(candles.size - 1, ((size.width - xOffset) / step).toInt())
            val labelCount = config.timeAxisLabelCount.coerceAtLeast(2)
            if (endIndex <= startIndex) return@Canvas
            repeat(labelCount) { labelIndex ->
                val index = startIndex + (endIndex - startIndex) * labelIndex / (labelCount - 1).coerceAtLeast(1)
                val x = index * step + xOffset + candleWidth / 2
                drawLine(colors.grid, Offset(x, 0f), Offset(x, 4.dp.toPx()), strokeWidth = 0.5.dp.toPx())
            }
        }
        if (candles.isNotEmpty()) {
            val step = candleWidth + candleSpacing
            val startIndex = max(0, ((-xOffset) / step).toInt())
            val endIndex = min(candles.size - 1, ((axisWidthPx - xOffset) / step).toInt())
            val labelCount = config.timeAxisLabelCount.coerceAtLeast(2)
            if (endIndex > startIndex) {
                repeat(labelCount) { labelIndex ->
                    val index = startIndex + (endIndex - startIndex) * labelIndex / (labelCount - 1).coerceAtLeast(1)
                    Text(
                        text = candles.getOrNull(index)?.timestamp?.let(config.timeLabelFormatter) ?: "",
                        color = colors.textSecondary,
                        fontSize = 9.sp,
                        lineHeight = 9.sp,
                        modifier = Modifier
                            .offset(x = with(LocalDensity.current) { (index * step + xOffset).toDp() })
                            .padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun PriceAxisLabels(
    minPrice: Double,
    maxPrice: Double,
    tickCount: Int,
    precision: Int,
    colors: KlineChartColors,
    modifier: Modifier,
) {
    BoxWithConstraints(modifier) {
        val ticks = tickCount.coerceAtLeast(2)
        repeat(ticks) { index ->
            val price = maxPrice - (maxPrice - minPrice) * index / (ticks - 1).toDouble()
            val y = maxHeight * index / (ticks - 1).toFloat()
            val yOffset = if (index == 0) y + 2.dp else y - 13.dp
            Text(
                text = price.formatChartValue(precision),
                color = colors.textSecondary,
                fontSize = 10.sp,
                lineHeight = 10.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(y = yOffset)
                    .padding(end = 4.dp),
            )
        }
    }
}

@Composable
internal fun LatestPriceLabel(
    price: Double,
    minPrice: Double,
    maxPrice: Double,
    precision: Int,
    colors: KlineChartColors,
    modifier: Modifier,
) {
    if (maxPrice <= minPrice) return
    BoxWithConstraints(modifier) {
        val labelHeight = 17.dp
        with(LocalDensity.current) {
            val heightPx = maxHeight.toPx()
            val labelHeightPx = labelHeight.toPx()
            val y = calculateValueY(price, minPrice, maxPrice, heightPx)
            val offsetY = (y - labelHeightPx / 2f)
                .coerceIn(0f, (heightPx - labelHeightPx).coerceAtLeast(0f))
                .toDp()
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(y = offsetY)
                    .padding(end = 2.dp)
                    .wrapContentHeight()
                    .height(labelHeight)
                    .background(colors.background, RoundedCornerShape(2.dp))
                    .border(0.5.dp, colors.textPrimary, RoundedCornerShape(2.dp))
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = price.formatChartValue(precision),
                    color = colors.textPrimary,
                    fontSize = 10.sp,
                    lineHeight = 10.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
