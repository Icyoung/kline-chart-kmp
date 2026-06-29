package io.github.icyoung

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Main chart configuration.
 *
 * This type keeps the original flat constructor for source compatibility.
 * New code can use the grouped constructor overload with [KlineLayoutConfig],
 * [KlineAxisConfig], [KlineInteractionConfig], [KlineIndicatorConfig], and
 * [KlineOverlayConfig] to avoid very large call sites.
 */
data class KlineChartConfig(
    val chartStyle: ChartStyle = ChartStyle.Candlestick,
    val pricePrecision: Int = 2,
    val baseCandleWidth: Float = 16f,
    val minCandleWidth: Float = 4f,
    val maxCandleWidth: Float = 50f,
    val candleSpacing: Float = 1f,
    val priceAxisWidth: Dp = 80.dp,
    val timeAxisHeight: Dp = 16.dp,
    val volumeHeight: Dp = 80.dp,
    val indicatorHeight: Dp = 120.dp,
    val showTimeAxis: Boolean = true,
    val showPriceAxis: Boolean = true,
    val showVolume: Boolean = true,
    val showLatestPriceLine: Boolean = true,
    val showLatestPriceLabel: Boolean = true,
    val showHighLowPriceLabels: Boolean = true,
    val showCrosshairInfoPanel: Boolean = true,
    val priceAxisTickCount: Int = 6,
    val timeAxisLabelCount: Int = 4,
    val lineStyle: KlineLineStyle = KlineLineStyle(),
    val markerStyle: KlineMarkerStyle = KlineMarkerStyle(),
    val volumePanel: VolumeIndicatorSpec = VolumeIndicatorSpec(),
    val candleRenderer: CandleRenderer = CandleRenderers.Candlestick,
    val mainIndicators: Set<MainIndicator> = setOf(MainIndicator.MA),
    val subIndicators: List<SubIndicator> = listOf(SubIndicator.MACD),
    val maPeriods: List<Int> = listOf(7, 25, 99),
    val emaPeriods: List<Int> = listOf(7, 25, 99),
    val volumeMaPeriods: List<Int> = listOf(5, 10),
    val rsiPeriods: List<Int> = listOf(6, 12, 24),
    val bollPeriod: Int = 20,
    val bollStdDevMultiplier: Double = 2.0,
    val macdFastPeriod: Int = 12,
    val macdSlowPeriod: Int = 26,
    val macdSignalPeriod: Int = 9,
    val kdjPeriod: Int = 9,
    val kdjKSmooth: Int = 3,
    val kdjDSmooth: Int = 3,
    val timeLabelFormatter: (timestamp: Long) -> String = { it.toString() },
    val crosshairLabelFormatter: ((CrosshairLabelContext) -> String?)? = null,
) {
    val layout: KlineLayoutConfig
        get() = KlineLayoutConfig(
            priceAxisWidth = priceAxisWidth,
            timeAxisHeight = timeAxisHeight,
            volumeHeight = volumeHeight,
            indicatorHeight = indicatorHeight,
        )

    val axis: KlineAxisConfig
        get() = KlineAxisConfig(
            showTimeAxis = showTimeAxis,
            showPriceAxis = showPriceAxis,
            priceAxisTickCount = priceAxisTickCount,
            timeAxisLabelCount = timeAxisLabelCount,
            timeLabelFormatter = timeLabelFormatter,
        )

    val interaction: KlineInteractionConfig
        get() = KlineInteractionConfig(
            baseCandleWidth = baseCandleWidth,
            minCandleWidth = minCandleWidth,
            maxCandleWidth = maxCandleWidth,
            candleSpacing = candleSpacing,
        )

    val indicators: KlineIndicatorConfig
        get() = KlineIndicatorConfig(
            mainIndicators = mainIndicators,
            subIndicators = subIndicators,
            maPeriods = maPeriods,
            emaPeriods = emaPeriods,
            volumeMaPeriods = volumeMaPeriods,
            rsiPeriods = rsiPeriods,
            bollPeriod = bollPeriod,
            bollStdDevMultiplier = bollStdDevMultiplier,
            macdFastPeriod = macdFastPeriod,
            macdSlowPeriod = macdSlowPeriod,
            macdSignalPeriod = macdSignalPeriod,
            kdjPeriod = kdjPeriod,
            kdjKSmooth = kdjKSmooth,
            kdjDSmooth = kdjDSmooth,
        )

    val overlays: KlineOverlayConfig
        get() = KlineOverlayConfig(
            showLatestPriceLine = showLatestPriceLine,
            showLatestPriceLabel = showLatestPriceLabel,
            showHighLowPriceLabels = showHighLowPriceLabels,
            showCrosshairInfoPanel = showCrosshairInfoPanel,
            crosshairLabelFormatter = crosshairLabelFormatter,
        )
}

/** Size-related chart layout configuration. */
data class KlineLayoutConfig(
    val priceAxisWidth: Dp = 80.dp,
    val timeAxisHeight: Dp = 16.dp,
    val volumeHeight: Dp = 80.dp,
    val indicatorHeight: Dp = 120.dp,
)

/** Axis visibility, tick density, and time formatting configuration. */
data class KlineAxisConfig(
    val showTimeAxis: Boolean = true,
    val showPriceAxis: Boolean = true,
    val priceAxisTickCount: Int = 6,
    val timeAxisLabelCount: Int = 4,
    val timeLabelFormatter: (timestamp: Long) -> String = { it.toString() },
)

/** Gesture and candle spacing configuration. */
data class KlineInteractionConfig(
    val baseCandleWidth: Float = 16f,
    val minCandleWidth: Float = 4f,
    val maxCandleWidth: Float = 50f,
    val candleSpacing: Float = 1f,
)

/** Built-in technical indicator configuration. */
data class KlineIndicatorConfig(
    val mainIndicators: Set<MainIndicator> = setOf(MainIndicator.MA),
    val subIndicators: List<SubIndicator> = listOf(SubIndicator.MACD),
    val maPeriods: List<Int> = listOf(7, 25, 99),
    val emaPeriods: List<Int> = listOf(7, 25, 99),
    val volumeMaPeriods: List<Int> = listOf(5, 10),
    val rsiPeriods: List<Int> = listOf(6, 12, 24),
    val bollPeriod: Int = 20,
    val bollStdDevMultiplier: Double = 2.0,
    val macdFastPeriod: Int = 12,
    val macdSlowPeriod: Int = 26,
    val macdSignalPeriod: Int = 9,
    val kdjPeriod: Int = 9,
    val kdjKSmooth: Int = 3,
    val kdjDSmooth: Int = 3,
)

/** Overlay and crosshair-related configuration. */
data class KlineOverlayConfig(
    val showLatestPriceLine: Boolean = true,
    val showLatestPriceLabel: Boolean = true,
    val showHighLowPriceLabels: Boolean = true,
    val showCrosshairInfoPanel: Boolean = true,
    val crosshairLabelFormatter: ((CrosshairLabelContext) -> String?)? = null,
)

data class KlineLineStyle(
    val lineWidth: Dp = 2.dp,
    val fillAlphaTop: Float = 0.28f,
    val fillAlphaBottom: Float = 0.0f,
    val color: Color? = null,
)

data class KlineMarkerStyle(
    val aggregation: KlineMarkerAggregation = KlineMarkerAggregation.ByCandleAndSide,
    val collision: KlineMarkerCollision = KlineMarkerCollision.Stack,
    val verticalOffsetPx: Float = 30f,
    val stackSpacingPx: Float = 18f,
)

enum class KlineMarkerAggregation {
    None,
    ByCandleAndSide,
}

enum class KlineMarkerCollision {
    Overlap,
    Stack,
}

/** Builds [KlineChartConfig] from grouped configuration objects. */
fun KlineChartConfig(
    chartStyle: ChartStyle = ChartStyle.Candlestick,
    pricePrecision: Int = 2,
    layout: KlineLayoutConfig = KlineLayoutConfig(),
    axis: KlineAxisConfig = KlineAxisConfig(),
    interaction: KlineInteractionConfig = KlineInteractionConfig(),
    indicators: KlineIndicatorConfig = KlineIndicatorConfig(),
    overlays: KlineOverlayConfig = KlineOverlayConfig(),
    lineStyle: KlineLineStyle = KlineLineStyle(),
    markerStyle: KlineMarkerStyle = KlineMarkerStyle(),
    showVolume: Boolean = true,
    volumePanel: VolumeIndicatorSpec = VolumeIndicatorSpec(),
    candleRenderer: CandleRenderer = CandleRenderers.Candlestick,
): KlineChartConfig {
    return KlineChartConfig(
        chartStyle = chartStyle,
        pricePrecision = pricePrecision,
        baseCandleWidth = interaction.baseCandleWidth,
        minCandleWidth = interaction.minCandleWidth,
        maxCandleWidth = interaction.maxCandleWidth,
        candleSpacing = interaction.candleSpacing,
        priceAxisWidth = layout.priceAxisWidth,
        timeAxisHeight = layout.timeAxisHeight,
        volumeHeight = layout.volumeHeight,
        indicatorHeight = layout.indicatorHeight,
        showTimeAxis = axis.showTimeAxis,
        showPriceAxis = axis.showPriceAxis,
        showVolume = showVolume,
        showLatestPriceLine = overlays.showLatestPriceLine,
        showLatestPriceLabel = overlays.showLatestPriceLabel,
        showHighLowPriceLabels = overlays.showHighLowPriceLabels,
        showCrosshairInfoPanel = overlays.showCrosshairInfoPanel,
        priceAxisTickCount = axis.priceAxisTickCount,
        timeAxisLabelCount = axis.timeAxisLabelCount,
        lineStyle = lineStyle,
        markerStyle = markerStyle,
        volumePanel = volumePanel,
        candleRenderer = candleRenderer,
        mainIndicators = indicators.mainIndicators,
        subIndicators = indicators.subIndicators,
        maPeriods = indicators.maPeriods,
        emaPeriods = indicators.emaPeriods,
        volumeMaPeriods = indicators.volumeMaPeriods,
        rsiPeriods = indicators.rsiPeriods,
        bollPeriod = indicators.bollPeriod,
        bollStdDevMultiplier = indicators.bollStdDevMultiplier,
        macdFastPeriod = indicators.macdFastPeriod,
        macdSlowPeriod = indicators.macdSlowPeriod,
        macdSignalPeriod = indicators.macdSignalPeriod,
        kdjPeriod = indicators.kdjPeriod,
        kdjKSmooth = indicators.kdjKSmooth,
        kdjDSmooth = indicators.kdjDSmooth,
        timeLabelFormatter = axis.timeLabelFormatter,
        crosshairLabelFormatter = overlays.crosshairLabelFormatter,
    )
}

enum class ChartStyle {
    Candlestick,
    Line,
}

data class KlineHistoryMarker(
    val timestamp: Long,
    val price: Double,
    val label: String,
    val placement: KlineMarkerPlacement,
)

enum class KlineMarkerPlacement {
    Below,
    Above,
}

data class MainIndicatorSpec(
    val id: String,
    val label: String,
    val preferredColorIndex: Int = 0,
    val parameters: List<KlineIndicatorParameterSpec> = emptyList(),
)

data class SubIndicatorSpec(
    val id: String,
    val label: String,
    val height: Dp? = null,
    val preferredColorIndex: Int = 0,
    val parameters: List<KlineIndicatorParameterSpec> = emptyList(),
)

data class KlineIndicatorParameterSpec(
    val id: String,
    val label: String,
    val defaultValue: Double,
    val range: ClosedFloatingPointRange<Double>,
    val step: Double = 1.0,
)

data class VolumeIndicatorSpec(
    val label: String = "VOL",
    val maPeriods: List<Int> = listOf(5, 10),
)

data class CrosshairLabelContext(
    val areaId: String,
    val yFraction: Float,
    val candleIndex: Int?,
    val candle: io.github.icyoung.model.OhlcvCandle?,
    val visibleMinPrice: Double,
    val visibleMaxPrice: Double,
    val macdRange: ClosedFloatingPointRange<Double>?,
)

enum class MainIndicator {
    MA,
    EMA,
    BOLL,
    SAR,
}

enum class SubIndicator {
    MACD,
    RSI,
    KDJ,
}

data class KlineChartColors(
    val background: Color = Color(0xFFFFFFFF),
    val grid: Color = Color(0xFFE5E7EB),
    val textPrimary: Color = Color(0xFF111827),
    val textSecondary: Color = Color(0xFF6B7280),
    val rising: Color = Color(0xFF16A34A),
    val falling: Color = Color(0xFFDC2626),
    val crosshair: Color = Color(0xFF111827),
    val labelBackground: Color = Color(0xFF111827),
    val labelText: Color = Color(0xFFFFFFFF),
    val indicator1: Color = Color(0xFFF59E0B),
    val indicator2: Color = Color(0xFF2563EB),
    val indicator3: Color = Color(0xFF9333EA),
    val indicator4: Color = Color(0xFF0891B2),
)

internal fun KlineChartColors.indicatorColor(index: Int): Color {
    return when (index % 4) {
        0 -> indicator1
        1 -> indicator2
        2 -> indicator3
        else -> indicator4
    }
}
