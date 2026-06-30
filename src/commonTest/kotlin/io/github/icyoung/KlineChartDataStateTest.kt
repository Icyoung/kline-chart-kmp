package io.github.icyoung

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
    fun updateReplacesExistingHistoricalTimestamp() {
        val state = KlineChartDataState(candles(3))
        val replacement = candle(1, close = 180.0)

        val result = state.update(replacement)

        assertEquals(KlineDataUpdateKind.ReplacedExisting, result)
        assertEquals(3, state.candles.size)
        assertEquals(180.0, state.candles[1].close)
    }

    @Test
    fun prependAddsOnlyOlderCandlesAndKeepsAscendingOrder() {
        val state = KlineChartDataState(candles(3, start = 3))

        val inserted = state.prepend(listOf(candle(2), candle(1), candle(3), candle(1)))

        assertEquals(2, inserted)
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), state.candles.map { it.timestamp })
        assertEquals(1, state.revision)
    }

    @Test
    fun updateIncrementsRevisionOnlyForAcceptedChanges() {
        val state = KlineChartDataState(candles(2))

        state.update(candle(-1))
        assertEquals(0, state.revision)

        state.update(candle(2))
        assertEquals(1, state.revision)

        state.replaceAll(candles(1))
        assertEquals(2, state.revision)
    }

    private fun candles(count: Int, start: Int = 0): List<OhlcvCandle> {
        return List(count) { candle(start + it) }
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
