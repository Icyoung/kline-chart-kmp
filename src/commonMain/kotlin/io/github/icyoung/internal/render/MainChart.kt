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
import io.github.icyoung.KlineIndicatorLine
import io.github.icyoung.LastPriceMode
import io.github.icyoung.KlineIndicatorPane
import io.github.icyoung.KlineIndicatorLineStyle
import io.github.icyoung.KlineIndicatorScaleMode
import io.github.icyoung.KlineIndicatorSeries
import io.github.icyoung.core.calculateVisibleRange
import io.github.icyoung.indicatorColor
import io.github.icyoung.internal.axis.TimeAxisTick
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
    timeTicks: List<TimeAxisTick>,
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

        drawGrid(colors.grid, chartWidth, size.height, timeTicks)
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
        var indicatorColorIndex = 0
        indicatorSeries
            .filter { it.pane == KlineIndicatorPane.Main }
            .forEach { series ->
                val range = when (series.scaleMode) {
                    KlineIndicatorScaleMode.SharedPriceAxis -> visible.minValue to visible.maxValue
                    KlineIndicatorScaleMode.IndependentAxis -> visibleMinMax(
                        candleWidth,
                        candleSpacing,
                        gestureState.xOffset,
                        chartWidth,
                        *series.lines.map { it.values }.toTypedArray(),
                    )
                }
                series.lines.forEachIndexed { lineIndex, line ->
                    drawMainIndicatorLine(
                        line = line,
                        visibleRange = renderRange,
                        candleWidth = candleWidth,
                        candleSpacing = candleSpacing,
                        xOffset = gestureState.xOffset,
                        minValue = range.first,
                        maxValue = range.second,
                        chartHeight = size.height,
                        color = colors.indicatorColor(indicatorColorIndex + lineIndex),
                    )
                }
                indicatorColorIndex += series.lines.size
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
        var latestValueColorIndex = 0
        indicatorSeries
            .filter { it.pane == KlineIndicatorPane.Main }
            .forEach { series ->
                if (series.showLatestValue) {
                    val range = when (series.scaleMode) {
                        KlineIndicatorScaleMode.SharedPriceAxis -> visible.minValue to visible.maxValue
                        KlineIndicatorScaleMode.IndependentAxis -> visibleMinMax(
                            candleWidth,
                            candleSpacing,
                            gestureState.xOffset,
                            chartWidth,
                            *series.lines.map { it.values }.toTypedArray(),
                        )
                    }
                    series.lines.forEachIndexed { lineIndex, line ->
                        val latest = line.latestVisibleValue(config.lastPriceMode, renderRange, candles.lastIndex)
                            ?: return@forEachIndexed
                        drawLatestValueLine(
                            value = latest.value,
                            valueIndex = latest.index,
                            candleWidth = candleWidth,
                            candleSpacing = candleSpacing,
                            xOffset = gestureState.xOffset,
                            minValue = range.first,
                            maxValue = range.second,
                            chartWidth = chartWidth,
                            color = colors.indicatorColor(latestValueColorIndex + lineIndex),
                        )
                    }
                }
                latestValueColorIndex += series.lines.size
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

internal data class KlineLatestIndicatorValue(
    val index: Int,
    val value: Double,
)

internal fun KlineIndicatorLine.latestVisibleValue(
    mode: LastPriceMode,
    visibleRange: IntRange,
    lastIndex: Int,
): KlineLatestIndicatorValue? {
    if (values.isEmpty() || lastIndex < 0) return null
    val targetIndex = when (mode) {
        LastPriceMode.Latest -> lastIndex
        LastPriceMode.RightmostVisible -> if (visibleRange.isEmpty()) lastIndex else visibleRange.last
    }.coerceIn(0, minOf(lastIndex, values.lastIndex))
    for (index in targetIndex downTo 0) {
        val value = values.getOrNull(index) ?: continue
        return KlineLatestIndicatorValue(index, value)
    }
    return null
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMainIndicatorLine(
    line: KlineIndicatorLine,
    visibleRange: IntRange,
    candleWidth: Float,
    candleSpacing: Float,
    xOffset: Float,
    minValue: Double,
    maxValue: Double,
    chartHeight: Float,
    color: androidx.compose.ui.graphics.Color,
) {
    when (line.style) {
        KlineIndicatorLineStyle.Line -> drawLineIndicator(
            values = line.values,
            visibleRange = visibleRange,
            candleWidth = candleWidth,
            candleSpacing = candleSpacing,
            xOffset = xOffset,
            minValue = minValue,
            maxValue = maxValue,
            chartHeight = chartHeight,
            color = color,
        )
        KlineIndicatorLineStyle.Points -> drawSar(
            values = line.values,
            visibleRange = visibleRange,
            candleWidth = candleWidth,
            candleSpacing = candleSpacing,
            xOffset = xOffset,
            minValue = minValue,
            maxValue = maxValue,
            chartHeight = chartHeight,
            color = color,
        )
        KlineIndicatorLineStyle.Histogram -> Unit
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
        indicatorSeries
            .filter { it.pane == KlineIndicatorPane.Main }
            .flatMap { it.lines }
            .forEachIndexed { index, line ->
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
