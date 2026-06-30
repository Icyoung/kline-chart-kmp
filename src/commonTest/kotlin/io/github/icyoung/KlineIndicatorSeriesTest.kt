package io.github.icyoung

import io.github.icyoung.model.OhlcvCandle
import kotlin.test.Test
import kotlin.test.assertEquals

class KlineIndicatorSeriesTest {
    @Test
    fun computesIncrementalTailWhenOnlyLastCandleChanges() {
        val cache = KlineIndicatorSeriesCache()
        val indicator = klineIndicator("close", KlineIndicatorPane.Sub, lookback = 2) { candles ->
            listOf(KlineIndicatorLine("C", candles.map { it.close }))
        }

        computeKlineIndicatorSeries(listOf(indicator), candles(3), cache)
        val result = computeKlineIndicatorSeries(listOf(indicator), candles(3, lastClose = 200.0), cache)

        assertEquals(listOf(100.0, 101.0, 200.0), result.single().lines.single().values)
    }

    @Test
    fun computesIncrementalTailWhenOneCandleAppends() {
        val cache = KlineIndicatorSeriesCache()
        val indicator = klineIndicator("close", KlineIndicatorPane.Sub, lookback = 2) { candles ->
            listOf(KlineIndicatorLine("C", candles.map { it.close }))
        }

        computeKlineIndicatorSeries(listOf(indicator), candles(3), cache)
        val result = computeKlineIndicatorSeries(listOf(indicator), candles(4), cache)

        assertEquals(listOf(100.0, 101.0, 102.0, 103.0), result.single().lines.single().values)
    }

    private fun candles(count: Int, lastClose: Double? = null): List<OhlcvCandle> {
        return List(count) { index ->
            val close = if (index == count - 1 && lastClose != null) lastClose else 100.0 + index
            OhlcvCandle(
                timestamp = index.toLong(),
                endTimestamp = index.toLong(),
                open = close - 1.0,
                high = close + 2.0,
                low = close - 2.0,
                close = close,
                volume = 1_000.0 + index,
            )
        }
    }
}
