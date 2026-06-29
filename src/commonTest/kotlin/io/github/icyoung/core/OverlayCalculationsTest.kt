package io.github.icyoung.core

import androidx.compose.ui.graphics.Color
import io.github.icyoung.KlineOverlayLine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class OverlayCalculationsTest {
    @Test
    fun convertsPriceAndYCoordinates() {
        val y = priceToY(150.0, minPrice = 100.0, maxPrice = 200.0, canvasHeight = 100f)

        assertEquals(50f, y)
        assertEquals(150.0, yToPrice(y, minPrice = 100.0, maxPrice = 200.0, canvasHeight = 100f))
    }

    @Test
    fun convertsIndexToX() {
        assertEquals(30f, indexToX(index = 2, candleWidth = 10f, candleSpacing = 2f, xOffset = 1f))
    }

    @Test
    fun hitTestsOverlayLineByPriceY() {
        val hit = hitTestOverlayLine(
            lines = listOf(KlineOverlayLine("line-1", price = 150.0, label = "150", color = Color.Red)),
            minPrice = 100.0,
            maxPrice = 200.0,
            canvasHeight = 100f,
            tapY = 51f,
        )

        assertNotNull(hit)
        assertEquals("line-1", hit.id)
    }
}
