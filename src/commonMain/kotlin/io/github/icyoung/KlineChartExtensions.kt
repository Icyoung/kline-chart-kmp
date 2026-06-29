package io.github.icyoung

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import io.github.icyoung.model.OhlcvCandle

/**
 * Describes a host-provided indicator that can be rendered in a custom panel.
 *
 * The current chart surface exposes custom panels as the rendering hook. This
 * type gives applications a stable shape for indicator calculation and rendering
 * while the built-in registration pipeline matures.
 */
data class KlineCustomIndicator(
    val id: String,
    val label: String,
    val preferredColor: Color? = null,
    val calculator: KlineIndicatorCalculator,
    val renderer: KlineIndicatorRenderer,
)

fun interface KlineIndicatorCalculator {
    fun calculate(candles: List<OhlcvCandle>): KlineIndicatorValues
}

data class KlineIndicatorValues(
    val series: Map<String, List<Double?>>,
    val minValue: Double? = null,
    val maxValue: Double? = null,
)

fun interface KlineIndicatorRenderer {
    fun DrawScope.render(context: KlineIndicatorRenderContext)
}

data class KlineIndicatorRenderContext(
    val candles: List<OhlcvCandle>,
    val values: KlineIndicatorValues,
    val visibleRange: IntRange,
    val candleWidth: Float,
    val candleSpacing: Float,
    val xOffset: Float,
    val minValue: Double,
    val maxValue: Double,
    val colors: KlineChartColors,
)
