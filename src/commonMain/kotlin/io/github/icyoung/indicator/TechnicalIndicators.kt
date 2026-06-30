package io.github.icyoung.indicator

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

object TechnicalIndicators {
    data class BollingerBands(
        val upper: List<Double?>,
        val middle: List<Double?>,
        val lower: List<Double?>,
    )

    data class MACD(
        val macdLine: List<Double?>,
        val signalLine: List<Double?>,
        val histogram: List<Double?>,
    )

    data class KDJ(
        val k: List<Double?>,
        val d: List<Double?>,
        val j: List<Double?>,
    )

    fun <T> calculateSMA(items: List<T>, period: Int, close: (T) -> Double): List<Double?> {
        if (items.size < period) return List(items.size) { null }

        return items.indices.map { index ->
            if (index < period - 1) {
                null
            } else {
                val window = items.subList(index - period + 1, index + 1)
                window.sumOf(close) / period.toDouble()
            }
        }
    }

    fun <T> calculateMA(items: List<T>, period: Int, close: (T) -> Double): List<Double?> {
        return calculateSMA(items, period, close)
    }

    fun <T> calculateEMA(items: List<T>, period: Int, close: (T) -> Double): List<Double?> {
        if (items.isEmpty()) return emptyList()
        if (items.size < period) return List(items.size) { null }

        val result = mutableListOf<Double?>()
        val alpha = 2.0 / (period.toDouble() + 1.0)

        repeat(period - 1) {
            result.add(null)
        }

        var ema = items.take(period).sumOf(close) / period.toDouble()
        result.add(ema)

        for (index in period until items.size) {
            ema = alpha * close(items[index]) + (1 - alpha) * ema
            result.add(ema)
        }

        return result
    }

    fun <T> calculateBollingerBands(
        items: List<T>,
        period: Int = 20,
        stdDevMultiplier: Double = 2.0,
        close: (T) -> Double,
    ): BollingerBands {
        val sma = calculateSMA(items, period, close)
        val upper = mutableListOf<Double?>()
        val lower = mutableListOf<Double?>()

        for (index in items.indices) {
            val mean = sma[index]
            if (mean == null) {
                upper.add(null)
                lower.add(null)
            } else {
                val values = items.subList(index - period + 1, index + 1).map(close)
                val variance = values.sumOf { value ->
                    val diff = value - mean
                    diff * diff
                } / period.toDouble()
                val stdDev = sqrt(variance)

                upper.add(mean + stdDevMultiplier * stdDev)
                lower.add(mean - stdDevMultiplier * stdDev)
            }
        }

        return BollingerBands(upper, sma, lower)
    }

    fun <T> calculateMACD(
        items: List<T>,
        fastPeriod: Int = 12,
        slowPeriod: Int = 26,
        signalPeriod: Int = 9,
        close: (T) -> Double,
    ): MACD {
        val emaFast = calculateEMA(items, fastPeriod, close)
        val emaSlow = calculateEMA(items, slowPeriod, close)

        val macdLine = items.indices.map { index ->
            if (emaFast[index] != null && emaSlow[index] != null) {
                emaFast[index]!! - emaSlow[index]!!
            } else {
                null
            }
        }
        val signalLine = calculateEMAFromValues(macdLine, signalPeriod)
        val histogram = items.indices.map { index ->
            if (macdLine[index] != null && signalLine[index] != null) {
                macdLine[index]!! - signalLine[index]!!
            } else {
                null
            }
        }

        return MACD(macdLine, signalLine, histogram)
    }

    fun <T> calculateRSI(items: List<T>, period: Int = 14, close: (T) -> Double): List<Double?> {
        if (items.size < period + 1) return List(items.size) { null }

        val result = mutableListOf<Double?>()
        var avgGain = 0.0
        var avgLoss = 0.0

        for (index in 1..period) {
            val change = close(items[index]) - close(items[index - 1])
            if (change > 0) {
                avgGain += change
            } else {
                avgLoss += abs(change)
            }
        }

        avgGain /= period
        avgLoss /= period

        repeat(period) {
            result.add(null)
        }

        val initialRs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
        result.add(100 - (100 / (1 + initialRs)))

        for (index in (period + 1) until items.size) {
            val change = close(items[index]) - close(items[index - 1])
            val gain = if (change > 0) change else 0.0
            val loss = if (change < 0) abs(change) else 0.0

            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period

            val rs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
            result.add(100 - (100 / (1 + rs)))
        }

        return result
    }

    fun <T> calculateKDJ(
        items: List<T>,
        period: Int = 9,
        kSmooth: Int = 3,
        dSmooth: Int = 3,
        high: (T) -> Double,
        low: (T) -> Double,
        close: (T) -> Double,
    ): KDJ {
        val kValues = mutableListOf<Double?>()
        val dValues = mutableListOf<Double?>()
        val jValues = mutableListOf<Double?>()
        var prevK = 50.0
        var prevD = 50.0

        for (index in items.indices) {
            if (index < period - 1) {
                kValues.add(null)
                dValues.add(null)
                jValues.add(null)
            } else {
                val window = items.subList(index - period + 1, index + 1)
                val lowest = window.minOf(low)
                val highest = window.maxOf(high)
                val rsv = if (highest == lowest) {
                    50.0
                } else {
                    (close(items[index]) - lowest) / (highest - lowest) * 100
                }
                val k = (rsv + (kSmooth - 1) * prevK) / kSmooth
                val d = (k + (dSmooth - 1) * prevD) / dSmooth
                val j = 3 * k - 2 * d

                kValues.add(k)
                dValues.add(d)
                jValues.add(j)
                prevK = k
                prevD = d
            }
        }

        return KDJ(kValues, dValues, jValues)
    }

    fun <T> calculateVolumeMA(items: List<T>, period: Int, volume: (T) -> Double): List<Double?> {
        if (items.size < period) return List(items.size) { null }

        return items.indices.map { index ->
            if (index < period - 1) {
                null
            } else {
                val window = items.subList(index - period + 1, index + 1)
                window.sumOf(volume) / period.toDouble()
            }
        }
    }

    fun <T> calculateWR(
        items: List<T>,
        period: Int = 14,
        high: (T) -> Double,
        low: (T) -> Double,
        close: (T) -> Double,
    ): List<Double?> {
        if (period <= 0) return List(items.size) { null }
        return items.indices.map { index ->
            if (index < period - 1) {
                null
            } else {
                val window = items.subList(index - period + 1, index + 1)
                val highest = window.maxOf(high)
                val lowest = window.minOf(low)
                if (highest == lowest) 0.0 else (highest - close(items[index])) / (highest - lowest) * 100.0
            }
        }
    }

    fun <T> calculateOBV(items: List<T>, close: (T) -> Double, volume: (T) -> Double): List<Double?> {
        if (items.isEmpty()) return emptyList()
        val result = MutableList<Double?>(items.size) { null }
        var obv = 0.0
        result[0] = obv
        for (index in 1 until items.size) {
            val change = close(items[index]) - close(items[index - 1])
            obv += when {
                change > 0.0 -> volume(items[index])
                change < 0.0 -> -volume(items[index])
                else -> 0.0
            }
            result[index] = obv
        }
        return result
    }

    fun <T> calculateSAR(
        items: List<T>,
        acceleration: Double = 0.02,
        maxAcceleration: Double = 0.2,
        high: (T) -> Double,
        low: (T) -> Double,
        close: (T) -> Double,
    ): List<Double?> {
        if (items.size < 2) return List(items.size) { null }

        val result = mutableListOf<Double?>()
        result.add(null)

        var isUpTrend = close(items[1]) > close(items[0])
        var sar = if (isUpTrend) low(items[0]) else high(items[0])
        var ep = if (isUpTrend) high(items[1]) else low(items[1])
        var af = acceleration

        for (index in 1 until items.size) {
            result.add(sar)

            val nextSar = sar + af * (ep - sar)
            if (isUpTrend) {
                if (low(items[index]) <= nextSar) {
                    isUpTrend = false
                    sar = ep
                    ep = low(items[index])
                    af = acceleration
                } else {
                    sar = nextSar
                    if (high(items[index]) > ep) {
                        ep = high(items[index])
                        af = min(af + acceleration, maxAcceleration)
                    }
                }
            } else {
                if (high(items[index]) >= nextSar) {
                    isUpTrend = true
                    sar = ep
                    ep = high(items[index])
                    af = acceleration
                } else {
                    sar = nextSar
                    if (low(items[index]) < ep) {
                        ep = low(items[index])
                        af = min(af + acceleration, maxAcceleration)
                    }
                }
            }
        }

        return result
    }

    private fun calculateEMAFromValues(values: List<Double?>, period: Int): List<Double?> {
        val result = mutableListOf<Double?>()
        val multiplier = 2.0 / (period.toDouble() + 1.0)
        var previousEMA: Double? = null
        var validCount = 0
        var sum = 0.0

        for (value in values) {
            if (value == null) {
                result.add(null)
            } else {
                if (previousEMA == null) {
                    sum += value
                    validCount++

                    if (validCount >= period) {
                        previousEMA = sum / period.toDouble()
                        result.add(previousEMA)
                    } else {
                        result.add(null)
                    }
                } else {
                    val currentEMA = (value - previousEMA) * multiplier + previousEMA
                    result.add(currentEMA)
                    previousEMA = currentEMA
                }
            }
        }

        return result
    }
}
