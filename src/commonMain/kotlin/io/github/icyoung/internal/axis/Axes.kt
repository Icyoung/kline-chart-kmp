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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.icyoung.KlineChartColors
import io.github.icyoung.KlineChartConfig
import io.github.icyoung.core.calculateValueY
import io.github.icyoung.model.OhlcvCandle
import io.github.icyoung.util.formatChartValue

@Composable
internal fun TimeAxis(
    candles: List<OhlcvCandle>,
    config: KlineChartConfig,
    timeTicks: List<TimeAxisTick>,
    colors: KlineChartColors,
    modifier: Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    BoxWithConstraints(modifier.background(colors.background)) {
        Canvas(Modifier.fillMaxSize()) {
            if (candles.isEmpty()) return@Canvas
            val tickHeight = 4.dp.toPx()
            val labelTop = tickHeight
            val stroke = 0.5.dp.toPx()
            val style = TextStyle(
                color = colors.textSecondary,
                fontSize = 9.sp,
                lineHeight = 9.sp,
            )
            timeTicks.forEach { tick ->
                if (tick.textAlpha <= 0f) return@forEach
                drawLine(
                    color = colors.grid.copy(alpha = colors.grid.alpha * tick.textAlpha),
                    start = Offset(tick.x, 0f),
                    end = Offset(tick.x, tickHeight),
                    strokeWidth = stroke,
                )
                val label = candles.getOrNull(tick.index)?.timestamp?.let(config.timeLabelFormatter).orEmpty()
                if (label.isEmpty()) return@forEach
                val measured = textMeasurer.measure(
                    text = label,
                    style = style.copy(color = colors.textSecondary.copy(alpha = colors.textSecondary.alpha * tick.textAlpha)),
                )
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(
                        x = tick.x - measured.size.width / 2f,
                        y = labelTop,
                    ),
                )
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
    color: Color = colors.textPrimary,
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
                    .border(0.5.dp, color, RoundedCornerShape(2.dp))
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = price.formatChartValue(precision),
                    color = color,
                    fontSize = 10.sp,
                    lineHeight = 10.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
