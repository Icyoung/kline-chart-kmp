package io.github.icyoung

import io.github.icyoung.indicator.TechnicalIndicators
import io.github.icyoung.model.OhlcvCandle
import kotlin.test.Test
import kotlin.test.assertEquals

class KlineChartDataStateTest {
    @Test
    fun updateAppendsWhenTimestampIsAfterLastCandle() {
        val state = KlineChartDataState(candles(3))

        val result = state.update(candle(3))

        assertEquals(KlineDataUpdateKind.Appended, result)
        assertEquals(4, state.candles.size)
        assertEquals(3L, state.candles.last().timestamp)
    }

    @Test
    fun updateReplacesLastWhenTimestampMatchesLastCandle() {
        val state = KlineChartDataState(candles(3))
        val replacement = candle(2, close = 200.0)

        val result = state.update(replacement)

        assertEquals(KlineDataUpdateKind.UpdatedLast, result)
        assertEquals(3, state.candles.size)
        assertEquals(200.0, state.candles.last().close)
    }

    @Test
    fun updateIgnoresOutOfOrderUnknownTimestamp() {
        val state = KlineChartDataState(candles(3))

        val result = state.update(candle(-1))

        assertEquals(KlineDataUpdateKind.IgnoredOutOfOrder, result)
        assertEquals(3, state.candles.size)
    }

    @Test
    fun appendedIndicatorCacheMatchesFullCalculation() {
        val state = KlineChartDataState(candles(30))
        state.indicators.ma(5)
        state.indicators.ema(5)
        state.indicators.macd(12, 26, 9)

        state.update(candle(30))

        assertEquals(
            TechnicalIndicators.calculateMA(state.candles, 5) { it.close },
            state.indicators.ma(5),
        )
        assertEquals(
            TechnicalIndicators.calculateEMA(state.candles, 5) { it.close },
            state.indicators.ema(5),
        )
        assertEquals(
            TechnicalIndicators.calculateMACD(state.candles, 12, 26, 9) { it.close }.histogram,
            state.indicators.macd(12, 26, 9).histogram,
        )
    }

    @Test
    fun updateLastIndicatorCacheMatchesFullCalculation() {
        val state = KlineChartDataState(candles(30))
        state.indicators.boll(20, 2.0)
        state.indicators.volumeMa(5)
        state.indicators.rsi(6)

        state.update(candle(29, close = 250.0, volume = 2_500.0))

        assertEquals(
            TechnicalIndicators.calculateBollingerBands(state.candles, 20, 2.0) { it.close }.middle,
            state.indicators.boll(20, 2.0).middle,
        )
        assertEquals(
            TechnicalIndicators.calculateVolumeMA(state.candles, 5) { it.volume },
            state.indicators.volumeMa(5),
        )
        assertEquals(
            TechnicalIndicators.calculateRSI(state.candles, 6) { it.close },
            state.indicators.rsi(6),
        )
    }

    private fun candles(count: Int): List<OhlcvCandle> {
        return List(count) { candle(it) }
    }

    private fun candle(index: Int, close: Double = 100.0 + index, volume: Double = 1_000.0 + index): OhlcvCandle {
        return OhlcvCandle(
            timestamp = index.toLong(),
            endTimestamp = index.toLong(),
            open = close - 1.0,
            high = close + 2.0,
            low = close - 2.0,
            close = close,
            volume = volume,
        )
    }
}
