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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.icyoung.KlineChartColors
import io.github.icyoung.KlineChartConfig
import io.github.icyoung.KlineCustomIndicator
import io.github.icyoung.KlineIndicatorLine
import io.github.icyoung.KlineIndicatorLineStyle
import io.github.icyoung.KlineIndicatorRenderContext
import io.github.icyoung.KlineIndicatorSeries
import io.github.icyoung.KlineIndicatorValues
import io.github.icyoung.SubIndicator
import io.github.icyoung.core.calculateValueY
import io.github.icyoung.indicatorColor
import io.github.icyoung.internal.render.drawLineIndicator
import io.github.icyoung.internal.render.drawSar
import io.github.icyoung.internal.render.drawVolumeBars
import io.github.icyoung.internal.render.drawVolumeLine
import io.github.icyoung.internal.render.formatCompact
import io.github.icyoung.internal.render.visibleIndexRange
import io.github.icyoung.internal.render.visibleMax
import io.github.icyoung.internal.render.visibleMinMax
import io.github.icyoung.model.OhlcvCandle
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private val GuideDashEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))

@Composable
internal fun CustomIndicatorPanel(
    indicator: KlineCustomIndicator,
    candles: List<OhlcvCandle>,
    values: KlineIndicatorValues?,
    colors: KlineChartColors,
    candleWidth: Float,
    candleSpacing: Float,
    xOffset: Float,
) {
    val indicatorValues = values ?: remember(candles, indicator) {
        indicator.calculator.calculate(candles)
    }
    val maxSeriesSize = indicatorValues.series.values.maxOfOrNull { it.size } ?: candles.size
    Box(Modifier.fillMaxSize().background(colors.background)) {
        IndicatorHeader(
            text = indicator.label,
            colors = colors,
            color = indicator.preferredColor ?: colors.indicator1,
        )
        Canvas(Modifier.fillMaxSize().padding(top = 20.dp).clipToBounds()) {
            val visible = visibleIndexRange(maxSeriesSize, candleWidth, candleSpacing, xOffset, size.width)
            val (minValue, maxValue) = indicatorValues.visibleRange(visible)
            with(indicator.renderer) {
                render(
                    KlineIndicatorRenderContext(
                        candles = candles,
                        values = indicatorValues,
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
    indicatorSeries: List<KlineIndicatorSeries>,
    candleWidth: Float,
    candleSpacing: Float,
    xOffset: Float,
) {
    val lines = indicatorSeries.firstOrNull { it.id == "VOL" }?.lines.orEmpty()
    Box(Modifier.fillMaxSize().background(colors.background)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(
                config.volumePanel.label,
                color = colors.textSecondary,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.padding(end = 8.dp),
            )
            lines.forEachIndexed { index, line ->
                line.values.lastOrNull { it != null }?.let {
                    Text(
                        "${line.name}:${it.formatCompact()}",
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
            lines.forEachIndexed { index, line ->
                drawVolumeLine(line.values, candleWidth, candleSpacing, xOffset, maxVolume, colors.indicatorColor(index))
            }
        }
    }
}

@Composable
internal fun SubIndicatorPanel(
    indicator: SubIndicator,
    config: KlineChartConfig,
    colors: KlineChartColors,
    indicatorSeries: List<KlineIndicatorSeries>,
    candleWidth: Float,
    candleSpacing: Float,
    xOffset: Float,
    onMacdRangeChanged: (Pair<Double, Double>) -> Unit,
) {
    val series = indicatorSeries.firstOrNull { it.id == indicator.name }
    GenericSubIndicatorPanel(
        indicator = indicator,
        config = config,
        colors = colors,
        series = series,
        candleWidth = candleWidth,
        candleSpacing = candleSpacing,
        xOffset = xOffset,
        onMacdRangeChanged = onMacdRangeChanged,
    )
}

@Composable
private fun GenericSubIndicatorPanel(
    indicator: SubIndicator,
    config: KlineChartConfig,
    colors: KlineChartColors,
    series: KlineIndicatorSeries?,
    candleWidth: Float,
    candleSpacing: Float,
    xOffset: Float,
    onMacdRangeChanged: (Pair<Double, Double>) -> Unit,
) {
    val lines = series?.lines.orEmpty()
    Box(Modifier.fillMaxSize().background(colors.background)) {
        IndicatorHeader(
            text = indicatorHeader(indicator, config),
            colors = colors,
            values = lines,
        )
        Canvas(Modifier.fillMaxSize().padding(top = 20.dp).clipToBounds()) {
            if (indicator == SubIndicator.RSI || indicator == SubIndicator.WR) {
                drawGuideLines(colors)
            }
            if (lines.isEmpty()) return@Canvas

            val range = when (indicator) {
                SubIndicator.RSI, SubIndicator.WR -> 0.0 to 100.0
                else -> visibleMinMax(
                    candleWidth,
                    candleSpacing,
                    xOffset,
                    size.width,
                    *lines.map { it.values }.toTypedArray(),
                )
            }
            if (indicator == SubIndicator.MACD) {
                onMacdRangeChanged(range)
            }

            val visible = visibleIndexRange(lines.first().values.size, candleWidth, candleSpacing, xOffset, size.width)
            lines.forEachIndexed { index, line ->
                drawIndicatorLine(
                    line = line,
                    visible = visible,
                    candleWidth = candleWidth,
                    candleSpacing = candleSpacing,
                    xOffset = xOffset,
                    minValue = range.first,
                    maxValue = range.second,
                    color = colors.indicatorColor(index),
                    colors = colors,
                )
            }
        }
    }
}

private fun indicatorHeader(indicator: SubIndicator, config: KlineChartConfig): String {
    return when (indicator) {
        SubIndicator.MACD -> "MACD(${config.macdFastPeriod},${config.macdSlowPeriod},${config.macdSignalPeriod})"
        SubIndicator.RSI -> "RSI"
        SubIndicator.KDJ -> "KDJ(${config.kdjPeriod},${config.kdjKSmooth},${config.kdjDSmooth})"
        SubIndicator.WR -> "WR"
        SubIndicator.OBV -> "OBV"
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGuideLines(colors: KlineChartColors) {
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
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawIndicatorLine(
    line: KlineIndicatorLine,
    visible: IntRange,
    candleWidth: Float,
    candleSpacing: Float,
    xOffset: Float,
    minValue: Double,
    maxValue: Double,
    color: Color,
    colors: KlineChartColors,
) {
    when (line.style) {
        KlineIndicatorLineStyle.Line -> drawLineIndicator(
            line.values,
            visible,
            candleWidth,
            candleSpacing,
            xOffset,
            minValue,
            maxValue,
            size.height,
            color,
        )
        KlineIndicatorLineStyle.Points -> drawSar(
            line.values,
            visible,
            candleWidth,
            candleSpacing,
            xOffset,
            minValue,
            maxValue,
            size.height,
            color,
        )
        KlineIndicatorLineStyle.Histogram -> drawHistogram(
            line.values,
            visible,
            candleWidth,
            candleSpacing,
            xOffset,
            minValue,
            maxValue,
            colors,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHistogram(
    values: List<Double?>,
    visible: IntRange,
    candleWidth: Float,
    candleSpacing: Float,
    xOffset: Float,
    minValue: Double,
    maxValue: Double,
    colors: KlineChartColors,
) {
    if (maxValue <= minValue) return
    val zeroY = calculateValueY(0.0, minValue, maxValue, size.height)
    visible.forEach { index ->
        val value = values.getOrNull(index) ?: return@forEach
        val x = index * (candleWidth + candleSpacing) + xOffset + candleWidth / 2
        val y = calculateValueY(value, minValue, maxValue, size.height)
        drawRoundRect(
            color = if (value >= 0.0) colors.rising else colors.falling,
            topLeft = Offset(x - candleWidth * 0.35f, min(y, zeroY)),
            size = Size(candleWidth * 0.7f, max(1f, abs(zeroY - y))),
            cornerRadius = CornerRadius(1f, 1f),
        )
    }
}

@Composable
private fun IndicatorHeader(
    text: String,
    colors: KlineChartColors,
    color: Color = colors.textSecondary,
    values: List<KlineIndicatorLine> = emptyList(),
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(
            text,
            color = color,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.padding(end = 8.dp),
        )
        values.forEachIndexed { index, line ->
            line.values.lastOrNull { it != null }?.let {
                Text(
                    "${line.name}:${it.formatCompact()}",
                    color = colors.indicatorColor(index),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }
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
