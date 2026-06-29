package io.github.icyoung.internal.panel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.icyoung.KlineChartColors
import io.github.icyoung.KlineChartConfig
import io.github.icyoung.KlineCustomIndicator
import io.github.icyoung.KlineIndicatorCache
import io.github.icyoung.KlineIndicatorRenderContext
import io.github.icyoung.KlineIndicatorValues
import io.github.icyoung.SubIndicator
import io.github.icyoung.indicator.TechnicalIndicators
import io.github.icyoung.indicatorColor
import io.github.icyoung.internal.render.drawGrid
import io.github.icyoung.internal.render.drawLineIndicator
import io.github.icyoung.internal.render.drawMacd
import io.github.icyoung.internal.render.drawVolumeBars
import io.github.icyoung.internal.render.drawVolumeLine
import io.github.icyoung.internal.render.formatCompact
import io.github.icyoung.internal.render.visibleIndexRange
import io.github.icyoung.internal.render.visibleMax
import io.github.icyoung.internal.render.visibleMinMax
import io.github.icyoung.model.OhlcvCandle

private val GuideDashEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))

@Composable
internal fun CustomIndicatorPanel(
    indicator: KlineCustomIndicator,
    candles: List<OhlcvCandle>,
    colors: KlineChartColors,
    candleWidth: Float,
    candleSpacing: Float,
    xOffset: Float,
) {
    val values = remember(candles, indicator) {
        indicator.calculator.calculate(candles)
    }
    val maxSeriesSize = values.series.values.maxOfOrNull { it.size } ?: candles.size
    Box(Modifier.fillMaxSize().background(colors.background)) {
        IndicatorHeader(
            text = indicator.label,
            colors = colors,
            color = indicator.preferredColor ?: colors.indicator1,
        )
        Canvas(Modifier.fillMaxSize().padding(top = 20.dp).clipToBounds()) {
            val visible = visibleIndexRange(maxSeriesSize, candleWidth, candleSpacing, xOffset, size.width)
            val (minValue, maxValue) = values.visibleRange(visible)
            with(indicator.renderer) {
                render(
                    KlineIndicatorRenderContext(
                        candles = candles,
                        values = values,
                        visibleRange = visible,
                        candleWidth = candleWidth,
                        candleSpacing = candleSpacing,
                        xOffset = xOffset,
                        minValue = minValue,
                        maxValue = maxValue,
                        colors = colors,
                    )
                )
            }
        }
    }
}

@Composable
internal fun VolumePanel(
    candles: List<OhlcvCandle>,
    config: KlineChartConfig,
    colors: KlineChartColors,
    indicatorCache: KlineIndicatorCache?,
    candleWidth: Float,
    candleSpacing: Float,
    xOffset: Float,
) {
    val volumeMas = remember(candles, config.volumePanel.maPeriods) {
        config.volumePanel.maPeriods.map { period ->
            period to (indicatorCache?.volumeMa(period) ?: TechnicalIndicators.calculateVolumeMA(candles, period) { it.volume })
        }
    }
    Box(Modifier.fillMaxSize().background(colors.background)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(
                config.volumePanel.label,
                color = colors.textSecondary,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.padding(end = 8.dp),
            )
            volumeMas.forEachIndexed { index, (period, values) ->
                values.lastOrNull { it != null }?.let {
                    Text(
                        "MA$period:${it.formatCompact()}",
                        color = colors.indicatorColor(index),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }
        }
        Canvas(Modifier.fillMaxSize().padding(top = 20.dp).clipToBounds()) {
            val maxVolume = visibleMax(candles, candleWidth, candleSpacing, xOffset, size.width) { it.volume }
                .coerceAtLeast(1.0)
            drawVolumeBars(candles, candleWidth, candleSpacing, xOffset, maxVolume, colors)
            volumeMas.forEachIndexed { index, (_, values) ->
                drawVolumeLine(values, candleWidth, candleSpacing, xOffset, maxVolume, colors.indicatorColor(index))
            }
        }
    }
}

@Composable
internal fun SubIndicatorPanel(
    indicator: SubIndicator,
    candles: List<OhlcvCandle>,
    config: KlineChartConfig,
    colors: KlineChartColors,
    indicatorCache: KlineIndicatorCache?,
    candleWidth: Float,
    candleSpacing: Float,
    xOffset: Float,
    onMacdRangeChanged: (Pair<Double, Double>) -> Unit,
) {
    Box(Modifier.fillMaxSize().background(colors.background)) {
        when (indicator) {
            SubIndicator.MACD -> MacdPanel(candles, config, colors, indicatorCache, candleWidth, candleSpacing, xOffset, onMacdRangeChanged)
            SubIndicator.RSI -> RsiPanel(candles, config, colors, indicatorCache, candleWidth, candleSpacing, xOffset)
            SubIndicator.KDJ -> KdjPanel(candles, config, colors, indicatorCache, candleWidth, candleSpacing, xOffset)
        }
    }
}

@Composable
private fun MacdPanel(
    candles: List<OhlcvCandle>,
    config: KlineChartConfig,
    colors: KlineChartColors,
    indicatorCache: KlineIndicatorCache?,
    candleWidth: Float,
    candleSpacing: Float,
    xOffset: Float,
    onRangeChanged: (Pair<Double, Double>) -> Unit,
) {
    val macd = remember(candles, config.macdFastPeriod, config.macdSlowPeriod, config.macdSignalPeriod) {
        indicatorCache?.macd(config.macdFastPeriod, config.macdSlowPeriod, config.macdSignalPeriod) ?: TechnicalIndicators.calculateMACD(candles, config.macdFastPeriod, config.macdSlowPeriod, config.macdSignalPeriod) {
            it.close
        }
    }
    IndicatorHeader("MACD(${config.macdFastPeriod},${config.macdSlowPeriod},${config.macdSignalPeriod})", colors)
    Canvas(Modifier.fillMaxSize().padding(top = 20.dp).clipToBounds()) {
        val range = visibleMinMax(candleWidth, candleSpacing, xOffset, size.width, macd.macdLine, macd.signalLine, macd.histogram)
        onRangeChanged(range)
        drawMacd(macd, candleWidth, candleSpacing, xOffset, range.first, range.second, colors)
    }
}

@Composable
private fun RsiPanel(
    candles: List<OhlcvCandle>,
    config: KlineChartConfig,
    colors: KlineChartColors,
    indicatorCache: KlineIndicatorCache?,
    candleWidth: Float,
    candleSpacing: Float,
    xOffset: Float,
) {
    val values = remember(candles, config.rsiPeriods) {
        config.rsiPeriods.map { it to (indicatorCache?.rsi(it) ?: TechnicalIndicators.calculateRSI(candles, it) { candle -> candle.close }) }
    }
    IndicatorHeader("RSI", colors)
    Canvas(Modifier.fillMaxSize().padding(top = 20.dp).clipToBounds()) {
        drawLine(
            color = colors.grid.copy(alpha = 0.7f),
            start = Offset(0f, size.height * 0.2f),
            end = Offset(size.width, size.height * 0.2f),
            strokeWidth = 0.5.dp.toPx(),
            pathEffect = GuideDashEffect,
        )
        drawLine(
            color = colors.grid.copy(alpha = 0.7f),
            start = Offset(0f, size.height * 0.8f),
            end = Offset(size.width, size.height * 0.8f),
            strokeWidth = 0.5.dp.toPx(),
            pathEffect = GuideDashEffect,
        )
        values.forEachIndexed { index, (_, list) ->
            drawLineIndicator(
                list,
                visibleIndexRange(list.size, candleWidth, candleSpacing, xOffset, size.width),
                candleWidth,
                candleSpacing,
                xOffset,
                0.0,
                100.0,
                size.height,
                colors.indicatorColor(index),
            )
        }
    }
}

@Composable
private fun KdjPanel(
    candles: List<OhlcvCandle>,
    config: KlineChartConfig,
    colors: KlineChartColors,
    indicatorCache: KlineIndicatorCache?,
    candleWidth: Float,
    candleSpacing: Float,
    xOffset: Float,
) {
    val kdj = remember(candles, config.kdjPeriod, config.kdjKSmooth, config.kdjDSmooth) {
        indicatorCache?.kdj(config.kdjPeriod, config.kdjKSmooth, config.kdjDSmooth) ?: TechnicalIndicators.calculateKDJ(
            candles,
            config.kdjPeriod,
            config.kdjKSmooth,
            config.kdjDSmooth,
            high = { it.high },
            low = { it.low },
            close = { it.close },
        )
    }
    IndicatorHeader("KDJ(${config.kdjPeriod},${config.kdjKSmooth},${config.kdjDSmooth})", colors)
    Canvas(Modifier.fillMaxSize().padding(top = 20.dp).clipToBounds()) {
        val range = visibleMinMax(candleWidth, candleSpacing, xOffset, size.width, kdj.k, kdj.d, kdj.j)
        val visible = visibleIndexRange(candles.size, candleWidth, candleSpacing, xOffset, size.width)
        drawLineIndicator(kdj.k, visible, candleWidth, candleSpacing, xOffset, range.first, range.second, size.height, colors.indicator1)
        drawLineIndicator(kdj.d, visible, candleWidth, candleSpacing, xOffset, range.first, range.second, size.height, colors.indicator2)
        drawLineIndicator(kdj.j, visible, candleWidth, candleSpacing, xOffset, range.first, range.second, size.height, colors.indicator3)
    }
}

@Composable
private fun IndicatorHeader(
    text: String,
    colors: KlineChartColors,
    color: Color = colors.textSecondary,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(text, color = color, style = MaterialTheme.typography.labelSmall)
    }
}

private fun KlineIndicatorValues.visibleRange(visible: IntRange): Pair<Double, Double> {
    if (minValue != null && maxValue != null && maxValue > minValue) {
        return minValue to maxValue
    }

    var min = Double.MAX_VALUE
    var max = -Double.MAX_VALUE
    series.values.forEach { values ->
        visible.forEach { index ->
            val value = values.getOrNull(index) ?: return@forEach
            if (value < min) min = value
            if (value > max) max = value
        }
    }
    if (min == Double.MAX_VALUE || max == -Double.MAX_VALUE) return 0.0 to 1.0
    if (max <= min) return (min - 1.0) to (max + 1.0)
    val padding = (max - min) * 0.1
    return (min - padding) to (max + padding)
}
