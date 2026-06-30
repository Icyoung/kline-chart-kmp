package io.github.icyoung

import androidx.compose.ui.unit.Dp
import io.github.icyoung.indicator.TechnicalIndicators
import io.github.icyoung.model.OhlcvCandle

enum class KlineIndicatorPane {
    Main,
    Sub,
}

enum class KlineIndicatorAlignment {
    Timestamp,
    Index,
}

enum class KlineIndicatorLineStyle {
    Line,
    Histogram,
    Points,
}

enum class KlineIndicatorScaleMode {
    SharedPriceAxis,
    IndependentAxis,
}

data class KlineIndicatorLine(
    val name: String,
    val values: List<Double?>,
    val style: KlineIndicatorLineStyle = KlineIndicatorLineStyle.Line,
)

data class KlineIndicatorSeries(
    val id: String,
    val pane: KlineIndicatorPane,
    val overlayId: String?,
    val label: String,
    val height: Dp?,
    val scaleMode: KlineIndicatorScaleMode,
    val showLatestValue: Boolean,
    val lines: List<KlineIndicatorLine>,
)

fun interface KlineIndicatorFormula {
    fun calculate(candles: List<OhlcvCandle>): List<KlineIndicatorLine>
}

data class KlineIndicator(
    val id: String,
    val pane: KlineIndicatorPane = KlineIndicatorPane.Sub,
    val overlayId: String? = null,
    val label: String = id,
    val height: Dp? = null,
    val sourceCandles: List<OhlcvCandle>? = null,
    val alignment: KlineIndicatorAlignment = KlineIndicatorAlignment.Timestamp,
    val scaleMode: KlineIndicatorScaleMode = KlineIndicatorScaleMode.SharedPriceAxis,
    val showLatestValue: Boolean = false,
    val lookback: Int = Int.MAX_VALUE,
    val formula: KlineIndicatorFormula,
) {
    fun compute(candles: List<OhlcvCandle>): KlineIndicatorSeries {
        val source = sourceCandles ?: candles
        val lines = formula.calculate(source)
        return KlineIndicatorSeries(
            id = id,
            pane = pane,
            overlayId = overlayId,
            label = label,
            height = height,
            scaleMode = scaleMode,
            showLatestValue = showLatestValue,
            lines = alignLines(lines, source, candles),
        )
    }
}

fun klineIndicator(
    id: String,
    pane: KlineIndicatorPane = KlineIndicatorPane.Sub,
    overlayId: String? = null,
    label: String = id,
    height: Dp? = null,
    sourceCandles: List<OhlcvCandle>? = null,
    alignment: KlineIndicatorAlignment = KlineIndicatorAlignment.Timestamp,
    scaleMode: KlineIndicatorScaleMode? = null,
    showLatestValue: Boolean = false,
    lookback: Int = Int.MAX_VALUE,
    formula: (List<OhlcvCandle>) -> List<KlineIndicatorLine>,
): KlineIndicator {
    return KlineIndicator(
        id = id,
        pane = pane,
        overlayId = overlayId,
        label = label,
        height = height,
        sourceCandles = sourceCandles,
        alignment = alignment,
        scaleMode = scaleMode ?: if (sourceCandles == null) {
            KlineIndicatorScaleMode.SharedPriceAxis
        } else {
            KlineIndicatorScaleMode.IndependentAxis
        },
        showLatestValue = showLatestValue,
        lookback = lookback,
        formula = KlineIndicatorFormula(formula),
    )
}

private fun KlineIndicator.alignLines(
    lines: List<KlineIndicatorLine>,
    source: List<OhlcvCandle>,
    target: List<OhlcvCandle>,
): List<KlineIndicatorLine> {
    if (source === target || sourceCandles == null) return lines
    return when (alignment) {
        KlineIndicatorAlignment.Index -> lines.map { line ->
            line.copy(values = target.indices.map { index -> line.values.getOrNull(index) })
        }
        KlineIndicatorAlignment.Timestamp -> {
            val sourceIndexByTimestamp = source.indices.associateBy { source[it].timestamp }
            lines.map { line ->
                line.copy(
                    values = target.map { candle ->
                        sourceIndexByTimestamp[candle.timestamp]?.let { sourceIndex ->
                            line.values.getOrNull(sourceIndex)
                        }
                    },
                )
            }
        }
    }
}

class KlineIndicatorSeriesCache {
    var candles: List<OhlcvCandle> = emptyList()
    var indicators: List<KlineIndicator> = emptyList()
    var series: List<KlineIndicatorSeries> = emptyList()
}

fun computeKlineIndicatorSeries(
    indicators: List<KlineIndicator>,
    candles: List<OhlcvCandle>,
    cache: KlineIndicatorSeriesCache? = null,
): List<KlineIndicatorSeries> {
    if (candles.isEmpty() || indicators.isEmpty()) {
        if (cache != null) {
            cache.candles = candles
            cache.indicators = indicators
            cache.series = emptyList()
        }
        return emptyList()
    }
    val previousCandles = cache?.candles.orEmpty()
    val previousSeries = cache?.series.orEmpty()
    val previousIndicators = cache?.indicators.orEmpty()

    val canIncrement = previousCandles.isNotEmpty() &&
        previousSeries.size == indicators.size &&
        previousIndicators == indicators &&
        candles.size >= previousCandles.size &&
        candles.size - previousCandles.size <= 1 &&
        candles.first().timestamp == previousCandles.first().timestamp &&
        candles.getOrNull(previousCandles.lastIndex)?.timestamp == previousCandles.last().timestamp

    val result = if (!canIncrement) {
        indicators.map { it.compute(candles) }
    } else {
        indicators.mapIndexed { index, indicator ->
            val previous = previousSeries[index]
            val lookback = indicator.lookback
            if (indicator.sourceCandles != null) return@mapIndexed indicator.compute(candles)
            if (lookback <= 0 || lookback >= candles.size) return@mapIndexed indicator.compute(candles)

            val window = candles.takeLast(lookback)
            val windowSeries = indicator.compute(window)
            if (windowSeries.lines.size != previous.lines.size) return@mapIndexed indicator.compute(candles)

            val sameSize = candles.size == previousCandles.size
            val lines = previous.lines.mapIndexed { lineIndex, line ->
                val latest = windowSeries.lines[lineIndex].values.lastOrNull()
                val base = if (sameSize) line.values.dropLast(1) else line.values
                line.copy(values = base + latest)
            }
            previous.copy(lines = lines)
        }
    }

    if (cache != null) {
        cache.candles = candles
        cache.indicators = indicators
        cache.series = result
    }
    return result
}

fun KlineChartConfig.builtInIndicators(): List<KlineIndicator> {
    val indicators = mutableListOf<KlineIndicator>()
    if (MainIndicator.MA in mainIndicators) {
        indicators += klineIndicator("MA", KlineIndicatorPane.Main, lookback = maPeriods.maxOrNull() ?: 1) { candles ->
            maPeriods.map { period ->
                KlineIndicatorLine("MA$period", TechnicalIndicators.calculateMA(candles, period) { it.close })
            }
        }
    }
    if (MainIndicator.EMA in mainIndicators) {
        indicators += klineIndicator("EMA", KlineIndicatorPane.Main) { candles ->
            emaPeriods.map { period ->
                KlineIndicatorLine("EMA$period", TechnicalIndicators.calculateEMA(candles, period) { it.close })
            }
        }
    }
    if (MainIndicator.BOLL in mainIndicators) {
        indicators += klineIndicator("BOLL", KlineIndicatorPane.Main, lookback = bollPeriod.coerceAtLeast(1)) { candles ->
            val boll = TechnicalIndicators.calculateBollingerBands(candles, bollPeriod, bollStdDevMultiplier) { it.close }
            listOf(
                KlineIndicatorLine("UP", boll.upper),
                KlineIndicatorLine("MB", boll.middle),
                KlineIndicatorLine("DN", boll.lower),
            )
        }
    }
    if (MainIndicator.SAR in mainIndicators) {
        indicators += klineIndicator("SAR", KlineIndicatorPane.Main) { candles ->
            listOf(
                KlineIndicatorLine(
                    "SAR",
                    TechnicalIndicators.calculateSAR(candles, high = { it.high }, low = { it.low }, close = { it.close }),
                    KlineIndicatorLineStyle.Points,
                )
            )
        }
    }
    if (showVolume) {
        indicators += klineIndicator("VOL", KlineIndicatorPane.Sub, lookback = volumeMaPeriods.maxOrNull() ?: 1) { candles ->
            volumeMaPeriods.map { period ->
                KlineIndicatorLine("MA$period", TechnicalIndicators.calculateVolumeMA(candles, period) { it.volume })
            }
        }
    }
    subIndicators.forEach { indicator ->
        when (indicator) {
            SubIndicator.MACD -> {
                indicators += klineIndicator("MACD", KlineIndicatorPane.Sub) { candles ->
                    val macd = TechnicalIndicators.calculateMACD(candles, macdFastPeriod, macdSlowPeriod, macdSignalPeriod) { it.close }
                    listOf(
                        KlineIndicatorLine("DIF", macd.macdLine),
                        KlineIndicatorLine("DEA", macd.signalLine),
                        KlineIndicatorLine("MACD", macd.histogram, KlineIndicatorLineStyle.Histogram),
                    )
                }
            }
            SubIndicator.RSI -> {
                indicators += klineIndicator("RSI", KlineIndicatorPane.Sub) { candles ->
                    rsiPeriods.map { period ->
                        KlineIndicatorLine("RSI$period", TechnicalIndicators.calculateRSI(candles, period) { it.close })
                    }
                }
            }
            SubIndicator.KDJ -> {
                indicators += klineIndicator("KDJ", KlineIndicatorPane.Sub) { candles ->
                    val kdj = TechnicalIndicators.calculateKDJ(candles, kdjPeriod, kdjKSmooth, kdjDSmooth, high = { it.high }, low = { it.low }, close = { it.close })
                    listOf(
                        KlineIndicatorLine("K", kdj.k),
                        KlineIndicatorLine("D", kdj.d),
                        KlineIndicatorLine("J", kdj.j),
                    )
                }
            }
            SubIndicator.WR -> {
                indicators += klineIndicator("WR", KlineIndicatorPane.Sub, lookback = wrPeriods.maxOrNull() ?: 1) { candles ->
                    wrPeriods.map { period ->
                        KlineIndicatorLine("WR$period", TechnicalIndicators.calculateWR(candles, period, high = { it.high }, low = { it.low }, close = { it.close }))
                    }
                }
            }
            SubIndicator.OBV -> {
                indicators += klineIndicator("OBV", KlineIndicatorPane.Sub) { candles ->
                    listOf(KlineIndicatorLine("OBV", TechnicalIndicators.calculateOBV(candles, close = { it.close }, volume = { it.volume })))
                }
            }
        }
    }
    return indicators
}

fun KlineCustomIndicator.asIndicator(): KlineIndicator {
    return klineIndicator(id = id, pane = KlineIndicatorPane.Sub, label = label) { candles ->
        calculator.calculate(candles).series.map { (name, values) ->
            KlineIndicatorLine(name, values)
        }
    }
}

fun KlineIndicatorSeries.toIndicatorValues(): KlineIndicatorValues {
    return KlineIndicatorValues(series = lines.associate { it.name to it.values })
}
