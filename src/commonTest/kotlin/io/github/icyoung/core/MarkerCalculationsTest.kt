package io.github.icyoung.core

import io.github.icyoung.KlineHistoryMarker
import io.github.icyoung.KlineMarkerAggregation
import io.github.icyoung.KlineMarkerPlacement
import io.github.icyoung.KlineMarkerStyle
import io.github.icyoung.model.OhlcvCandle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkerCalculationsTest {
    private val candles = listOf(
        candle(1_000, 1_999),
        candle(2_000, 2_999),
        candle(3_000, 3_999),
    )

    @Test
    fun findsCandleContainingTimestamp() {
        assertEquals(0, findCandleIndexForTimestamp(candles, 1_000))
        assertEquals(1, findCandleIndexForTimestamp(candles, 2_500))
        assertEquals(2, findCandleIndexForTimestamp(candles, 3_999))
    }

    @Test
    fun returnsMinusOneWhenTimestampIsOutsideAllCandles() {
        assertEquals(-1, findCandleIndexForTimestamp(candles, 999))
        assertEquals(-1, findCandleIndexForTimestamp(candles, 4_000))
    }

    @Test
    fun returnsMarkersInsideCandleTimeRange() {
        val markers = listOf(
            marker(1_500, "A"),
            marker(2_100, "B"),
            marker(2_900, "C"),
        )

        val result = markersForCandle(candles[1], markers)

        assertEquals(listOf("B", "C"), result.map { it.label })
    }

    @Test
    fun handlesEmptyInputs() {
        assertEquals(-1, findCandleIndexForTimestamp(emptyList(), 1_000))
        assertTrue(markersForCandle(candles.first(), emptyList()).isEmpty())
    }

    @Test
    fun hitTestsMarkerNearRenderedPosition() {
        val marker = marker(2_500, "B")

        val hit = hitTestHistoryMarker(
            markers = listOf(marker),
            candles = candles,
            candleWidth = 10f,
            candleSpacing = 2f,
            xOffset = 0f,
            minPrice = 0.0,
            maxPrice = 2.0,
            canvasHeight = 100f,
            tapX = 18f,
            tapY = 105f,
        )

        assertEquals(marker, hit)
    }

    @Test
    fun hitTestReturnsNullWhenTapIsOutsideMarker() {
        val hit = hitTestHistoryMarker(
            markers = listOf(marker(2_500, "B")),
            candles = candles,
            candleWidth = 10f,
            candleSpacing = 2f,
            xOffset = 0f,
            minPrice = 0.0,
            maxPrice = 2.0,
            canvasHeight = 100f,
            tapX = 200f,
            tapY = 200f,
        )

        assertEquals(null, hit)
    }

    @Test
    fun aggregatesMarkersByCandleAndSide() {
        val layouts = calculateMarkerLayouts(
            markers = listOf(marker(2_100, "B"), marker(2_200, "B")),
            candles = candles,
            candleWidth = 10f,
            candleSpacing = 2f,
            xOffset = 0f,
            minPrice = 0.0,
            maxPrice = 2.0,
            canvasHeight = 100f,
            style = KlineMarkerStyle(aggregation = KlineMarkerAggregation.ByCandleAndSide),
        )

        assertEquals(1, layouts.size)
        assertEquals("B x2", layouts.first().label)
    }

    @Test
    fun keepsMarkersSeparateWhenAggregationDisabled() {
        val layouts = calculateMarkerLayouts(
            markers = listOf(marker(2_100, "B"), marker(2_200, "B")),
            candles = candles,
            candleWidth = 10f,
            candleSpacing = 2f,
            xOffset = 0f,
            minPrice = 0.0,
            maxPrice = 2.0,
            canvasHeight = 100f,
            style = KlineMarkerStyle(aggregation = KlineMarkerAggregation.None),
        )

        assertEquals(2, layouts.size)
        assertEquals(true, layouts[1].centerY > layouts[0].centerY)
    }

    private fun candle(start: Long, end: Long): OhlcvCandle {
        return OhlcvCandle(
            timestamp = start,
            endTimestamp = end,
            open = 1.0,
            high = 2.0,
            low = 0.5,
            close = 1.5,
            volume = 100.0,
        )
    }

    private fun marker(timestamp: Long, label: String): KlineHistoryMarker {
        return KlineHistoryMarker(
            timestamp = timestamp,
            price = 1.0,
            label = label,
            placement = KlineMarkerPlacement.Below,
        )
    }
}
