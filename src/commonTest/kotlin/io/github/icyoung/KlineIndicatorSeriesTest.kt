package io.github.icyoung

import androidx.compose.ui.unit.dp
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

    @Test
    fun preservesIndicatorPlacementMetadataInComputedSeries() {
        val indicator = klineIndicator(
            id = "custom",
            pane = KlineIndicatorPane.Sub,
            overlayId = "MACD",
            label = "Custom",
            height = 120.dp,
        ) { candles ->
            listOf(KlineIndicatorLine("C", candles.map { it.close }))
        }

        val series = computeKlineIndicatorSeries(listOf(indicator), candles(2)).single()

        assertEquals("custom", series.id)
        assertEquals(KlineIndicatorPane.Sub, series.pane)
        assertEquals("MACD", series.overlayId)
        assertEquals("Custom", series.label)
        assertEquals(120.dp, series.height)
        assertEquals(KlineIndicatorScaleMode.SharedPriceAxis, series.scaleMode)
        assertEquals(false, series.showLatestValue)
    }

    @Test
    fun alignsSourceCandlesByTimestampByDefault() {
        val chartCandles = candles(5)
        val sourceCandles = listOf(candle(1, 201.0), candle(3, 203.0), candle(4, 204.0))
        val indicator = klineIndicator(
            id = "btc-close",
            pane = KlineIndicatorPane.Main,
            sourceCandles = sourceCandles,
        ) { candles ->
            listOf(KlineIndicatorLine("BTC", candles.map { it.close }))
        }

        val series = computeKlineIndicatorSeries(listOf(indicator), chartCandles).single()

        assertEquals(listOf(null, 201.0, null, 203.0, 204.0), series.lines.single().values)
        assertEquals(KlineIndicatorScaleMode.IndependentAxis, series.scaleMode)
    }

    @Test
    fun canAlignSourceCandlesByIndex() {
        val chartCandles = candles(5)
        val sourceCandles = listOf(candle(10, 210.0), candle(20, 220.0), candle(30, 230.0))
        val indicator = klineIndicator(
            id = "btc-index-close",
            pane = KlineIndicatorPane.Main,
            sourceCandles = sourceCandles,
            alignment = KlineIndicatorAlignment.Index,
        ) { candles ->
            listOf(KlineIndicatorLine("BTC", candles.map { it.close }))
        }

        val series = computeKlineIndicatorSeries(listOf(indicator), chartCandles).single()

        assertEquals(listOf(210.0, 220.0, 230.0, null, null), series.lines.single().values)
    }

    @Test
    fun canOverrideSourceCandleScaleMode() {
        val indicator = klineIndicator(
            id = "external-shared",
            pane = KlineIndicatorPane.Main,
            sourceCandles = candles(2),
            scaleMode = KlineIndicatorScaleMode.SharedPriceAxis,
        ) { candles ->
            listOf(KlineIndicatorLine("C", candles.map { it.close }))
        }

        val series = computeKlineIndicatorSeries(listOf(indicator), candles(2)).single()

        assertEquals(KlineIndicatorScaleMode.SharedPriceAxis, series.scaleMode)
    }

    @Test
    fun preservesLatestValueVisibilityInComputedSeries() {
        val indicator = klineIndicator(
            id = "latest-line",
            pane = KlineIndicatorPane.Main,
            showLatestValue = true,
        ) { candles ->
            listOf(KlineIndicatorLine("C", candles.map { it.close }))
        }

        val series = computeKlineIndicatorSeries(listOf(indicator), candles(2)).single()

        assertEquals(true, series.showLatestValue)
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

    private fun candle(timestamp: Long, close: Double): OhlcvCandle {
        return OhlcvCandle(
            timestamp = timestamp,
            endTimestamp = timestamp,
            open = close - 1.0,
            high = close + 2.0,
            low = close - 2.0,
            close = close,
            volume = 1_000.0,
        )
    }
}
