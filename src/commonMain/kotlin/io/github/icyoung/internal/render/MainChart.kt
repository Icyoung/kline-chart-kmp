package io.github.icyoung.internal.render

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.icyoung.ChartStyle
import io.github.icyoung.CandleRenderContext
import io.github.icyoung.KlineHistoryMarker
import io.github.icyoung.KlineChartColors
import io.github.icyoung.KlineChartConfig
import io.github.icyoung.LastPriceMode
import io.github.icyoung.KlineIndicatorLineStyle
import io.github.icyoung.KlineIndicatorSeries
import io.github.icyoung.MainIndicator
import io.github.icyoung.core.calculateVisibleRange
import io.github.icyoung.indicatorColor
import io.github.icyoung.internal.gesture.ChartGestureState
import io.github.icyoung.model.OhlcvCandle
import io.github.icyoung.util.formatChartValue

@Composable
internal fun MainChartCanvas(
    candles: List<OhlcvCandle>,
    config: KlineChartConfig,
    colors: KlineChartColors,
    historyMarkers: List<KlineHistoryMarker>,
    indicatorSeries: List<KlineIndicatorSeries>,
    entranceProgress: Float,
    gestureState: ChartGestureState,
    candleWidth: Float,
    candleSpacing: Float,
    onRangeChanged: (Double, Double) -> Unit,
) {
    val textMeasurer = rememberTextMeasurer()
    val labelTextStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)

    Canvas(Modifier.fillMaxSize().clipToBounds()) {
        gestureState.updateCanvasSize(size)
        if (candles.isEmpty()) return@Canvas

        val chartWidth = size.width
        val visible = calculateVisibleRange(
            candles = candles,
            candleWidth = candleWidth,
            candleSpacing = candleSpacing,
            xOffset = gestureState.xOffset,
            canvasWidth = chartWidth,
            low = { it.low },
            high = { it.high },
            close = { it.close },
        )
        onRangeChanged(visible.minValue, visible.maxValue)
        val renderRange = visible.indices.revealedBy(entranceProgress)

        drawGrid(colors.grid, chartWidth, size.height)
        when (config.chartStyle) {
            ChartStyle.Candlestick -> {
                with(config.candleRenderer) {
                    render(
                        CandleRenderContext(
                            candles = candles,
                            visibleRange = renderRange,
                            candleWidth = candleWidth,
                            candleSpacing = candleSpacing,
                            xOffset = gestureState.xOffset,
                            minPrice = visible.minValue,
                            maxPrice = visible.maxValue,
                            chartHeight = size.height,
                            colors = colors,
                        )
                    )
                }
            }
            ChartStyle.Line -> {
                drawLineSeries(
                    candles = candles,
                    visibleRange = renderRange,
                    candleWidth = candleWidth,
                    candleSpacing = candleSpacing,
                    xOffset = gestureState.xOffset,
                    minPrice = visible.minValue,
                    maxPrice = visible.maxValue,
                    style = config.lineStyle,
                    fallbackColor = colors.indicator2,
                )
            }
        }
        if (MainIndicator.MA in config.mainIndicators) {
            indicatorSeries.firstOrNull { it.id == "MA" }?.lines.orEmpty().forEachIndexed { index, line ->
                drawLineIndicator(
                    values = line.values,
                    visibleRange = renderRange,
                    candleWidth = candleWidth,
                    candleSpacing = candleSpacing,
                    xOffset = gestureState.xOffset,
                    minValue = visible.minValue,
                    maxValue = visible.maxValue,
                    chartHeight = size.height,
                    color = colors.indicatorColor(index),
                )
            }
        }
        if (MainIndicator.EMA in config.mainIndicators) {
            indicatorSeries.firstOrNull { it.id == "EMA" }?.lines.orEmpty().forEachIndexed { index, line ->
                drawLineIndicator(
                    values = line.values,
                    visibleRange = renderRange,
                    candleWidth = candleWidth,
                    candleSpacing = candleSpacing,
                    xOffset = gestureState.xOffset,
                    minValue = visible.minValue,
                    maxValue = visible.maxValue,
                    chartHeight = size.height,
                    color = colors.indicatorColor(index + 1),
                )
            }
        }
        if (MainIndicator.BOLL in config.mainIndicators) {
            indicatorSeries.firstOrNull { it.id == "BOLL" }?.lines.orEmpty().forEachIndexed { index, line ->
                drawLineIndicator(line.values, renderRange, candleWidth, candleSpacing, gestureState.xOffset, visible.minValue, visible.maxValue, size.height, colors.indicatorColor(index))
            }
        }
        if (MainIndicator.SAR in config.mainIndicators) {
            indicatorSeries.firstOrNull { it.id == "SAR" }?.lines?.firstOrNull()?.let { line ->
                if (line.style == KlineIndicatorLineStyle.Points) {
                    drawSar(line.values, renderRange, candleWidth, candleSpacing, gestureState.xOffset, visible.minValue, visible.maxValue, size.height, colors.indicator4)
                }
            }
        }
        if (config.showLatestPriceLine) {
            val latestPriceIndex = when (config.lastPriceMode) {
                LastPriceMode.Latest -> candles.lastIndex
                LastPriceMode.RightmostVisible -> renderRange.last.coerceIn(candles.indices)
            }
            if (entranceProgress >= 1f || latestPriceIndex in renderRange) {
                drawLatestPriceLine(
                    candles = candles,
                    candleWidth = candleWidth,
                    candleSpacing = candleSpacing,
                    xOffset = gestureState.xOffset,
                    price = candles[latestPriceIndex].close,
                    minPrice = visible.minValue,
                    maxPrice = visible.maxValue,
                    chartWidth = chartWidth,
                    color = colors.textPrimary,
                    candleIndex = latestPriceIndex,
                )
            }
        }
        if (config.showHighLowPriceLabels && config.chartStyle == ChartStyle.Candlestick) {
            drawHighLowLabels(
                candles = candles,
                visibleRange = renderRange,
                candleWidth = candleWidth,
                candleSpacing = candleSpacing,
                xOffset = gestureState.xOffset,
                minPrice = visible.minValue,
                maxPrice = visible.maxValue,
                chartWidth = chartWidth,
                precision = config.pricePrecision,
                color = colors.textSecondary,
                textMeasurer = textMeasurer,
                style = labelTextStyle,
            )
        }
        drawHistoryMarkers(
            markers = historyMarkers,
            candles = candles,
            candleWidth = candleWidth,
            candleSpacing = candleSpacing,
            xOffset = gestureState.xOffset,
            minPrice = visible.minValue,
            maxPrice = visible.maxValue,
            textMeasurer = textMeasurer,
            style = labelTextStyle,
            colors = colors,
            markerStyle = config.markerStyle,
        )
        if (config.showPriceAxis) {
            drawPriceAxis(chartWidth, size.height, colors)
        }
    }
}

private fun IntRange.revealedBy(progress: Float): IntRange {
    if (isEmpty()) return this
    val clamped = progress.coerceIn(0f, 1f)
    val count = count()
    val visibleCount = (count * clamped).toInt().coerceIn(1, count)
    return first..(first + visibleCount - 1)
}

@Composable
internal fun MainIndicatorLabels(
    candles: List<OhlcvCandle>,
    config: KlineChartConfig,
    colors: KlineChartColors,
    indicatorSeries: List<KlineIndicatorSeries>,
    modifier: Modifier,
) {
    if (candles.isEmpty()) return
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        val style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
        if (MainIndicator.MA in config.mainIndicators) {
            indicatorSeries.firstOrNull { it.id == "MA" }?.lines.orEmpty().forEachIndexed { index, line ->
                val value = line.values.lastOrNull { it != null }
                if (value != null) {
                    Text(
                        text = "${line.name}:${value.formatChartValue(config.pricePrecision)}",
                        color = colors.indicatorColor(index),
                        style = style,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }
        }
    }
}
