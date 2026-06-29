package io.github.icyoung.internal.render

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import io.github.icyoung.MainIndicator
import io.github.icyoung.KlineIndicatorCache
import io.github.icyoung.core.calculateVisibleRange
import io.github.icyoung.indicator.TechnicalIndicators
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
    indicatorCache: KlineIndicatorCache?,
    gestureState: ChartGestureState,
    candleWidth: Float,
    candleSpacing: Float,
    onRangeChanged: (Double, Double) -> Unit,
) {
    val maValues = remember(candles, config.maPeriods) {
        config.maPeriods.map { period ->
            period to (indicatorCache?.ma(period) ?: TechnicalIndicators.calculateMA(candles, period) { it.close })
        }
    }
    val emaValues = remember(candles, config.emaPeriods) {
        config.emaPeriods.map { period ->
            period to (indicatorCache?.ema(period) ?: TechnicalIndicators.calculateEMA(candles, period) { it.close })
        }
    }
    val boll = remember(candles, config.bollPeriod, config.bollStdDevMultiplier) {
        indicatorCache?.boll(config.bollPeriod, config.bollStdDevMultiplier) ?: TechnicalIndicators.calculateBollingerBands(
            candles,
            config.bollPeriod,
            config.bollStdDevMultiplier,
        ) { it.close }
    }
    val sar = remember(candles) {
        indicatorCache?.sar() ?: TechnicalIndicators.calculateSAR(candles, high = { it.high }, low = { it.low }, close = { it.close })
    }
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

        drawGrid(colors.grid, chartWidth, size.height)
        when (config.chartStyle) {
            ChartStyle.Candlestick -> {
                with(config.candleRenderer) {
                    render(
                        CandleRenderContext(
                            candles = candles,
                            visibleRange = visible.indices,
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
                    visibleRange = visible.indices,
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
            maValues.forEachIndexed { index, (_, values) ->
                drawLineIndicator(
                    values = values,
                    visibleRange = visible.indices,
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
            emaValues.forEachIndexed { index, (_, values) ->
                drawLineIndicator(
                    values = values,
                    visibleRange = visible.indices,
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
            drawLineIndicator(boll.upper, visible.indices, candleWidth, candleSpacing, gestureState.xOffset, visible.minValue, visible.maxValue, size.height, colors.indicator1)
            drawLineIndicator(boll.middle, visible.indices, candleWidth, candleSpacing, gestureState.xOffset, visible.minValue, visible.maxValue, size.height, colors.indicator2)
            drawLineIndicator(boll.lower, visible.indices, candleWidth, candleSpacing, gestureState.xOffset, visible.minValue, visible.maxValue, size.height, colors.indicator3)
        }
        if (MainIndicator.SAR in config.mainIndicators) {
            drawSar(sar, visible.indices, candleWidth, candleSpacing, gestureState.xOffset, visible.minValue, visible.maxValue, size.height, colors.indicator4)
        }
        if (config.showLatestPriceLine) {
            drawLatestPriceLine(
                candles = candles,
                candleWidth = candleWidth,
                candleSpacing = candleSpacing,
                xOffset = gestureState.xOffset,
                price = candles.last().close,
                minPrice = visible.minValue,
                maxPrice = visible.maxValue,
                chartWidth = chartWidth,
                color = colors.textPrimary,
            )
        }
        if (config.showHighLowPriceLabels && config.chartStyle == ChartStyle.Candlestick) {
            drawHighLowLabels(
                candles = candles,
                visibleRange = visible.indices,
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

@Composable
internal fun MainIndicatorLabels(
    candles: List<OhlcvCandle>,
    config: KlineChartConfig,
    colors: KlineChartColors,
    indicatorCache: KlineIndicatorCache?,
    modifier: Modifier,
) {
    if (candles.isEmpty()) return
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        val style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
        if (MainIndicator.MA in config.mainIndicators) {
            config.maPeriods.forEachIndexed { index, period ->
                val values = remember(candles, period) {
                    indicatorCache?.ma(period) ?: TechnicalIndicators.calculateMA(candles, period) { it.close }
                }
                val value = values.lastOrNull { it != null }
                if (value != null) {
                    Text(
                        text = "MA$period:${value.formatChartValue(config.pricePrecision)}",
                        color = colors.indicatorColor(index),
                        style = style,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }
        }
    }
}
