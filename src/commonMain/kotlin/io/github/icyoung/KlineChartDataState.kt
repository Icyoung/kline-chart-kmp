package io.github.icyoung

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.icyoung.indicator.TechnicalIndicators
import io.github.icyoung.model.OhlcvCandle
import kotlin.math.sqrt

@Composable
fun rememberKlineChartDataState(initialCandles: List<OhlcvCandle> = emptyList()): KlineChartDataState {
    return remember { KlineChartDataState(initialCandles) }
}

@Stable
class KlineChartDataState(initialCandles: List<OhlcvCandle> = emptyList()) {
    private val indicatorCache = KlineIndicatorCache(initialCandles)

    var candles by mutableStateOf(initialCandles)
        private set

    var revision by mutableIntStateOf(0)
        private set

    val indicators: KlineIndicatorCache get() = indicatorCache

    fun replaceAll(newCandles: List<OhlcvCandle>) {
        indicatorCache.replaceAll(newCandles)
        candles = newCandles
        revision++
    }

    fun update(candle: OhlcvCandle): KlineDataUpdateKind {
        val result = indicatorCache.update(candle)
        candles = when (result) {
            KlineDataUpdateKind.Appended -> candles + candle
            KlineDataUpdateKind.UpdatedLast -> candles.toMutableList().also { it[it.lastIndex] = candle }
            KlineDataUpdateKind.ReplacedExisting -> candles.toMutableList().also { list ->
                val index = list.indexOfFirst { it.timestamp == candle.timestamp }
                if (index >= 0) list[index] = candle
            }
            KlineDataUpdateKind.IgnoredOutOfOrder -> candles
        }
        if (result != KlineDataUpdateKind.IgnoredOutOfOrder) {
            revision++
        }
        return result
    }
}

enum class KlineDataUpdateKind {
    UpdatedLast,
    Appended,
    ReplacedExisting,
    IgnoredOutOfOrder,
}

class KlineIndicatorCache(initialCandles: List<OhlcvCandle> = emptyList()) {
    private var candles: List<OhlcvCandle> = initialCandles
    private val series = mutableMapOf<String, MutableList<Double?>>()
    private val bollSeries = mutableMapOf<String, TechnicalIndicators.BollingerBands>()
    private val macdSeries = mutableMapOf<String, TechnicalIndicators.MACD>()
    private val kdjSeries = mutableMapOf<String, TechnicalIndicators.KDJ>()
    private var lastChange: DataChange = DataChange.ReplaceAll

    fun replaceAll(newCandles: List<OhlcvCandle>) {
        candles = newCandles
        series.clear()
        bollSeries.clear()
        macdSeries.clear()
        kdjSeries.clear()
        lastChange = DataChange.ReplaceAll
    }

    fun update(candle: OhlcvCandle): KlineDataUpdateKind {
        if (candles.isEmpty()) {
            candles = listOf(candle)
            lastChange = DataChange.Append(0)
            updateCachedTail()
            return KlineDataUpdateKind.Appended
        }

        val last = candles.last()
        return when {
            candle.timestamp == last.timestamp -> {
                updateLastInternal(candle)
                KlineDataUpdateKind.UpdatedLast
            }
            candle.timestamp > last.timestamp -> {
                appendInternal(candle)
                KlineDataUpdateKind.Appended
            }
            else -> {
                val index = candles.indexOfFirst { it.timestamp == candle.timestamp }
                if (index < 0) {
                    KlineDataUpdateKind.IgnoredOutOfOrder
                } else {
                    replaceExisting(index, candle)
                    KlineDataUpdateKind.ReplacedExisting
                }
            }
        }
    }

    @Deprecated("Use update(candle); it chooses update-last or append by timestamp.")
    fun updateLast(candle: OhlcvCandle) {
        update(candle)
    }

    @Deprecated("Use update(candle); it chooses update-last or append by timestamp.")
    fun append(candle: OhlcvCandle) {
        update(candle)
    }

    private fun updateLastInternal(candle: OhlcvCandle) {
        if (candles.isEmpty()) {
            replaceAll(listOf(candle))
            return
        }
        candles = candles.toMutableList().also { it[it.lastIndex] = candle }
        lastChange = DataChange.UpdateLast(candles.lastIndex)
        updateCachedTail()
    }

    private fun appendInternal(candle: OhlcvCandle) {
        val index = candles.size
        candles = candles + candle
        lastChange = DataChange.Append(index)
        updateCachedTail()
    }

    private fun replaceExisting(index: Int, candle: OhlcvCandle) {
        candles = candles.toMutableList().also { it[index] = candle }
        lastChange = DataChange.UpdateFrom(index)
        updateCachedTail()
    }

    fun ma(period: Int): List<Double?> {
        return movingAverage("ma:$period", period) { it.close }
    }

    fun ema(period: Int): List<Double?> {
        val key = "ema:$period"
        val cached = series[key]
        if (cached == null) {
            return TechnicalIndicators.calculateEMA(candles, period) { it.close }.toMutableList().also { series[key] = it }
        }
        cached.ensureSize(candles.size)
        recomputeEmaTail(cached, period)
        return cached
    }

    fun boll(period: Int, multiplier: Double): TechnicalIndicators.BollingerBands {
        val key = "boll:$period:$multiplier"
        val cached = bollSeries[key]
        if (cached == null) {
            return TechnicalIndicators.calculateBollingerBands(candles, period, multiplier) { it.close }
                .also { bollSeries[key] = it }
        }
        val mutableUpper = cached.upper.toMutableList()
        val mutableMiddle = cached.middle.toMutableList()
        val mutableLower = cached.lower.toMutableList()
        mutableUpper.ensureSize(candles.size)
        mutableMiddle.ensureSize(candles.size)
        mutableLower.ensureSize(candles.size)
        val start = affectedIndex().coerceAtLeast(0)
        for (index in start until candles.size) {
            val values = bollAt(index, period, multiplier)
            mutableUpper[index] = values?.first
            mutableMiddle[index] = values?.second
            mutableLower[index] = values?.third
        }
        return TechnicalIndicators.BollingerBands(mutableUpper, mutableMiddle, mutableLower)
            .also { bollSeries[key] = it }
    }

    fun sar(): List<Double?> {
        val key = "sar"
        val cached = series[key]
        if (cached == null) {
            return TechnicalIndicators.calculateSAR(candles, high = { it.high }, low = { it.low }, close = { it.close })
                .toMutableList()
                .also { series[key] = it }
        }
        cached.ensureSize(candles.size)
        val start = affectedIndex()
        val recomputed = TechnicalIndicators.calculateSAR(candles.subList(0, candles.size), high = { it.high }, low = { it.low }, close = { it.close })
        for (index in start until candles.size) cached[index] = recomputed[index]
        return cached
    }

    fun volumeMa(period: Int): List<Double?> {
        return movingAverage("volumeMa:$period", period) { it.volume }
    }

    fun macd(fastPeriod: Int, slowPeriod: Int, signalPeriod: Int): TechnicalIndicators.MACD {
        val key = "macd:$fastPeriod:$slowPeriod:$signalPeriod"
        val cached = macdSeries[key]
        if (cached == null) {
            return TechnicalIndicators.calculateMACD(candles, fastPeriod, slowPeriod, signalPeriod) { it.close }
                .also { macdSeries[key] = it }
        }
        val start = affectedIndex()
        val macd = cached.macdLine.toMutableList()
        val signal = cached.signalLine.toMutableList()
        val histogram = cached.histogram.toMutableList()
        macd.ensureSize(candles.size)
        signal.ensureSize(candles.size)
        histogram.ensureSize(candles.size)
        val fast = ema(fastPeriod)
        val slow = ema(slowPeriod)
        for (index in start until candles.size) {
            macd[index] = if (fast[index] != null && slow[index] != null) fast[index]!! - slow[index]!! else null
            signal[index] = signalAt(index, signalPeriod, macd, signal)
            histogram[index] = if (macd[index] != null && signal[index] != null) macd[index]!! - signal[index]!! else null
        }
        return TechnicalIndicators.MACD(macd, signal, histogram).also { macdSeries[key] = it }
    }

    fun rsi(period: Int): List<Double?> {
        val key = "rsi:$period"
        val cached = series[key]
        if (cached == null) {
            return TechnicalIndicators.calculateRSI(candles, period) { it.close }.toMutableList().also { series[key] = it }
        }
        cached.ensureSize(candles.size)
        val recomputed = TechnicalIndicators.calculateRSI(candles, period) { it.close }
        val start = affectedIndex()
        for (index in start until candles.size) cached[index] = recomputed[index]
        return cached
    }

    fun kdj(period: Int, kSmooth: Int, dSmooth: Int): TechnicalIndicators.KDJ {
        val key = "kdj:$period:$kSmooth:$dSmooth"
        val cached = kdjSeries[key]
        if (cached == null) {
            return TechnicalIndicators.calculateKDJ(candles, period, kSmooth, dSmooth, high = { it.high }, low = { it.low }, close = { it.close })
                .also { kdjSeries[key] = it }
        }
        val start = affectedIndex()
        val k = cached.k.toMutableList()
        val d = cached.d.toMutableList()
        val j = cached.j.toMutableList()
        k.ensureSize(candles.size)
        d.ensureSize(candles.size)
        j.ensureSize(candles.size)
        for (index in start until candles.size) {
            val values = kdjAt(index, period, kSmooth, dSmooth, k, d)
            k[index] = values?.first
            d[index] = values?.second
            j[index] = values?.third
        }
        return TechnicalIndicators.KDJ(k, d, j).also { kdjSeries[key] = it }
    }

    private fun updateCachedTail() {
        val start = affectedIndex()
        series.forEach { (key, values) ->
            values.ensureSize(candles.size)
            when {
                key.startsWith("ma:") -> recomputeMovingAverageTail(values, key.removePrefix("ma:").toInt()) { it.close }
                key.startsWith("volumeMa:") -> recomputeMovingAverageTail(values, key.removePrefix("volumeMa:").toInt()) { it.volume }
                key.startsWith("ema:") -> recomputeEmaTail(values, key.removePrefix("ema:").toInt())
                key == "sar" -> {
                    val recomputed = TechnicalIndicators.calculateSAR(candles, high = { it.high }, low = { it.low }, close = { it.close })
                    for (index in start until candles.size) values[index] = recomputed[index]
                }
                key.startsWith("rsi:") -> {
                    val recomputed = TechnicalIndicators.calculateRSI(candles, key.removePrefix("rsi:").toInt()) { it.close }
                    for (index in start until candles.size) values[index] = recomputed[index]
                }
            }
        }
        bollSeries.keys.toList().forEach { key ->
            val (_, periodRaw, multiplierRaw) = key.split(":")
            boll(periodRaw.toInt(), multiplierRaw.toDouble())
        }
        macdSeries.keys.toList().forEach { key ->
            val (_, fast, slow, signal) = key.split(":")
            macd(fast.toInt(), slow.toInt(), signal.toInt())
        }
        kdjSeries.keys.toList().forEach { key ->
            val (_, period, kSmooth, dSmooth) = key.split(":")
            kdj(period.toInt(), kSmooth.toInt(), dSmooth.toInt())
        }
    }

    private fun movingAverage(key: String, period: Int, value: (OhlcvCandle) -> Double): List<Double?> {
        val cached = series[key]
        if (cached == null) {
            return candles.indices.map { movingAverageAt(it, period, value) }.toMutableList().also { series[key] = it }
        }
        cached.ensureSize(candles.size)
        recomputeMovingAverageTail(cached, period, value)
        return cached
    }

    private fun recomputeMovingAverageTail(values: MutableList<Double?>, period: Int, value: (OhlcvCandle) -> Double) {
        val start = affectedIndex()
        for (index in start until candles.size) {
            values[index] = movingAverageAt(index, period, value)
        }
    }

    private fun recomputeEmaTail(values: MutableList<Double?>, period: Int) {
        val start = affectedIndex()
        val alpha = 2.0 / (period + 1.0)
        for (index in start until candles.size) {
            values[index] = when {
                index < period - 1 -> null
                index == period - 1 -> candles.take(period).sumOf { it.close } / period
                else -> values[index - 1]?.let { previous ->
                    alpha * candles[index].close + (1 - alpha) * previous
                }
            }
        }
    }

    private fun movingAverageAt(index: Int, period: Int, value: (OhlcvCandle) -> Double): Double? {
        if (index < period - 1) return null
        var sum = 0.0
        for (cursor in index - period + 1..index) {
            sum += value(candles[cursor])
        }
        return sum / period
    }

    private fun bollAt(index: Int, period: Int, multiplier: Double): Triple<Double, Double, Double>? {
        val mean = movingAverageAt(index, period) { it.close } ?: return null
        var variance = 0.0
        for (cursor in index - period + 1..index) {
            val diff = candles[cursor].close - mean
            variance += diff * diff
        }
        val stdDev = sqrt(variance / period)
        return Triple(mean + multiplier * stdDev, mean, mean - multiplier * stdDev)
    }

    private fun signalAt(
        index: Int,
        period: Int,
        macd: List<Double?>,
        signal: List<Double?>,
    ): Double? {
        val value = macd[index] ?: return null
        val alpha = 2.0 / (period + 1.0)
        val previous = signal.getOrNull(index - 1)
        if (previous != null) return alpha * value + (1 - alpha) * previous

        val validValues = macd.take(index + 1).filterNotNull()
        if (validValues.size < period) return null
        return validValues.takeLast(period).average()
    }

    private fun kdjAt(
        index: Int,
        period: Int,
        kSmooth: Int,
        dSmooth: Int,
        kValues: List<Double?>,
        dValues: List<Double?>,
    ): Triple<Double, Double, Double>? {
        if (index < period - 1) return null
        val start = index - period + 1
        val window = candles.subList(start, index + 1)
        val lowest = window.minOf { it.low }
        val highest = window.maxOf { it.high }
        val rsv = if (highest == lowest) 50.0 else (candles[index].close - lowest) / (highest - lowest) * 100
        val previousK = kValues.getOrNull(index - 1) ?: 50.0
        val previousD = dValues.getOrNull(index - 1) ?: 50.0
        val k = (rsv + (kSmooth - 1) * previousK) / kSmooth
        val d = (k + (dSmooth - 1) * previousD) / dSmooth
        return Triple(k, d, 3 * k - 2 * d)
    }

    private fun affectedIndex(): Int {
        return when (val change = lastChange) {
            DataChange.ReplaceAll -> 0
            is DataChange.UpdateLast -> change.index
            is DataChange.Append -> change.index
            is DataChange.UpdateFrom -> change.index
        }.coerceIn(0, candles.size)
    }

    private sealed interface DataChange {
        data object ReplaceAll : DataChange
        data class UpdateLast(val index: Int) : DataChange
        data class Append(val index: Int) : DataChange
        data class UpdateFrom(val index: Int) : DataChange
    }

    private fun MutableList<Double?>.ensureSize(size: Int) {
        while (this.size < size) add(null)
        while (this.size > size) removeAt(lastIndex)
    }
}
