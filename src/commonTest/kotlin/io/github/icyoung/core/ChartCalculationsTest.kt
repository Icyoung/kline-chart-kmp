package io.github.icyoung.core

import io.github.icyoung.indicator.TechnicalIndicators
import io.github.icyoung.model.OhlcvCandle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChartCalculationsTest {
    private val candles = List(30) { index ->
        val base = 100.0 + index
        OhlcvCandle(
            timestamp = index.toLong(),
            open = base,
            high = base + 2.0,
            low = base - 2.0,
            close = base + 1.0,
            volume = 1_000.0 + index,
        )
    }

    @Test
    fun visibleRangeIncludesPricePadding() {
        val range = calculateVisibleRange(
            candles = candles,
            candleWidth = 10f,
            candleSpacing = 1f,
            xOffset = 0f,
            canvasWidth = 120f,
            low = { it.low },
            high = { it.high },
            close = { it.close },
        )

        assertTrue(range.indices.first <= 0)
        assertTrue(range.indices.last > range.indices.first)
        assertTrue(range.minValue < candles.first().low)
        assertTrue(range.maxValue > candles[range.indices.last].high)
    }

    @Test
    fun scrollBoundsCenterShortSeries() {
        val bounds = calculateScrollBounds(
            itemCount = 3,
            candleWidth = 10f,
            candleSpacing = 1f,
            canvasWidth = 100f,
        )

        assertEquals(bounds.first, bounds.second)
        assertEquals(33.5f, bounds.first)
    }

    @Test
    fun movingAverageProducesLeadingNulls() {
        val ma = TechnicalIndicators.calculateMA(candles, period = 3) { it.close }

        assertEquals(null, ma[0])
        assertEquals(null, ma[1])
        assertEquals((101.0 + 102.0 + 103.0) / 3.0, ma[2])
    }
}
