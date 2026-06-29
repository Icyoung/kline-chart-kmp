package io.github.icyoung

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import io.github.icyoung.core.indexToX
import io.github.icyoung.core.priceToY
import io.github.icyoung.core.yToPrice
import io.github.icyoung.model.OhlcvCandle

@Stable
class KlineChartState internal constructor() {
    var zoom by mutableStateOf(1f)
        internal set
    var xOffset by mutableStateOf(0f)
        internal set
    var canvasSize by mutableStateOf(Size.Zero)
        internal set
    var visiblePriceRange by mutableStateOf(0.0..100.0)
        internal set
    var crosshair by mutableStateOf<CrosshairState?>(null)
        internal set
}

@androidx.compose.runtime.Composable
fun rememberKlineChartState(): KlineChartState {
    return androidx.compose.runtime.remember { KlineChartState() }
}

data class CrosshairState(
    val candleIndex: Int,
    val candle: OhlcvCandle,
    val position: Offset,
    val absoluteY: Float,
    val areaId: String,
    val label: String?,
)

data class KlineChartOverlayScope(
    val candles: List<OhlcvCandle>,
    val state: KlineChartState,
    val candleWidth: Float,
    val candleSpacing: Float,
    val visibleMinPrice: Double,
    val visibleMaxPrice: Double,
    val chartHeight: Float,
) {
    val xOffset: Float get() = state.xOffset

    fun priceToY(price: Double): Float = priceToY(price, visibleMinPrice, visibleMaxPrice, chartHeight)

    fun yToPrice(y: Float): Double = yToPrice(y, visibleMinPrice, visibleMaxPrice, chartHeight)

    fun indexToX(index: Int): Float = indexToX(index, candleWidth, candleSpacing, xOffset)

    fun timestampToX(timestamp: Long): Float? {
        val index = candles.indexOfFirst { timestamp in it.timestamp..it.endTimestamp }
        return index.takeIf { it >= 0 }?.let(::indexToX)
    }
}

data class KlineCrosshairInfoScope(
    val candle: OhlcvCandle,
    val candleIndex: Int,
    val candles: List<OhlcvCandle>,
    val historyMarkers: List<KlineHistoryMarker>,
    val state: KlineChartState,
    val crosshairX: Float,
    val canvasWidth: Float,
    val config: KlineChartConfig,
)
