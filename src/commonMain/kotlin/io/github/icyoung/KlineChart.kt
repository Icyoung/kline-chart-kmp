package io.github.icyoung

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.icyoung.core.calculateScrollBounds
import io.github.icyoung.core.KlineMarkerHitTestConfig
import io.github.icyoung.core.hitTestHistoryMarker
import io.github.icyoung.core.hitTestOverlayLine
import io.github.icyoung.internal.axis.LatestPriceLabel
import io.github.icyoung.internal.axis.PriceAxisLabels
import io.github.icyoung.internal.axis.TimeAxis
import io.github.icyoung.internal.axis.TimeAxisAnchorSet
import io.github.icyoung.internal.axis.calculateTimeAxisAnchorSet
import io.github.icyoung.internal.axis.projectTimeAxisTicks
import io.github.icyoung.internal.gesture.ChartGestureState
import io.github.icyoung.internal.gesture.klineGestures
import io.github.icyoung.internal.layout.ChartPanel
import io.github.icyoung.internal.overlay.CrosshairInfoPanel
import io.github.icyoung.internal.overlay.CrosshairOverlay
import io.github.icyoung.internal.overlay.OverlayLineLayer
import io.github.icyoung.internal.panel.CustomIndicatorPanel
import io.github.icyoung.internal.panel.IndicatorSeriesPanel
import io.github.icyoung.internal.panel.SubIndicatorPanel
import io.github.icyoung.internal.panel.VolumePanel
import io.github.icyoung.internal.render.MainChartCanvas
import io.github.icyoung.internal.render.MainIndicatorLabels
import io.github.icyoung.internal.render.latestVisibleValue
import io.github.icyoung.internal.render.visibleIndexRange
import io.github.icyoung.internal.render.visibleMinMax
import io.github.icyoung.model.OhlcvCandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Renders a Compose Multiplatform OHLCV/K-line chart.
 *
 * The chart owns gesture handling, built-in panels, axes, crosshair rendering,
 * and common market annotations. Host applications can add host-specific UI
 * through [overlayContent], [crosshairInfoContent], [customPanels], and
 * [historyMarkers] without depending on app-specific models.
 */
@Composable
fun KlineChart(
    dataState: KlineChartDataState,
    modifier: Modifier = Modifier,
    state: KlineChartState = rememberKlineChartState(),
    config: KlineChartConfig = KlineChartConfig(),
    colors: KlineChartColors = KlineChartColors(),
    historyMarkers: List<KlineHistoryMarker> = emptyList(),
    overlayLines: List<KlineOverlayLine> = emptyList(),
    overlayStyle: KlineOverlayStyle = KlineOverlayStyle(),
    indicators: List<KlineIndicator> = emptyList(),
    customIndicators: List<KlineCustomIndicator> = emptyList(),
    customPanels: List<KlinePanelSpec> = emptyList(),
    onOverlayLineClick: (KlineOverlayLineHit) -> Unit = {},
    onHistoryMarkerClick: (KlineHistoryMarker) -> Unit = {},
    onLoadMoreHistoricalData: () -> Unit = {},
    overlayContent: @Composable KlineChartOverlayScope.() -> Unit = {},
    crosshairInfoContent: (@Composable KlineCrosshairInfoScope.() -> Unit)? = null,
) {
    KlineChart(
        candles = dataState.candles,
        modifier = modifier,
        state = state,
        config = config,
        colors = colors,
        historyMarkers = historyMarkers,
        overlayLines = overlayLines,
        overlayStyle = overlayStyle,
        indicators = indicators,
        customIndicators = customIndicators,
        customPanels = customPanels,
        onOverlayLineClick = onOverlayLineClick,
        onHistoryMarkerClick = onHistoryMarkerClick,
        onLoadMoreHistoricalData = onLoadMoreHistoricalData,
        overlayContent = overlayContent,
        crosshairInfoContent = crosshairInfoContent,
    )
}

private fun KlineMarkerStyle.toHitTestConfig(): KlineMarkerHitTestConfig {
    return KlineMarkerHitTestConfig(verticalOffsetPx = verticalOffsetPx)
}

@Composable
fun KlineChart(
    candles: List<OhlcvCandle>,
    modifier: Modifier = Modifier,
    state: KlineChartState = rememberKlineChartState(),
    config: KlineChartConfig = KlineChartConfig(),
    colors: KlineChartColors = KlineChartColors(),
    historyMarkers: List<KlineHistoryMarker> = emptyList(),
    overlayLines: List<KlineOverlayLine> = emptyList(),
    overlayStyle: KlineOverlayStyle = KlineOverlayStyle(),
    indicators: List<KlineIndicator> = emptyList(),
    customIndicators: List<KlineCustomIndicator> = emptyList(),
    customPanels: List<KlinePanelSpec> = emptyList(),
    onOverlayLineClick: (KlineOverlayLineHit) -> Unit = {},
    onHistoryMarkerClick: (KlineHistoryMarker) -> Unit = {},
    onLoadMoreHistoricalData: () -> Unit = {},
    overlayContent: @Composable KlineChartOverlayScope.() -> Unit = {},
    crosshairInfoContent: (@Composable KlineCrosshairInfoScope.() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val gestureState = remember {
        ChartGestureState(
            scope = scope,
            density = density,
            onLoadMore = onLoadMoreHistoricalData,
        )
    }
    val candleWidth = (config.baseCandleWidth * gestureState.zoom)
        .coerceIn(config.minCandleWidth, config.maxCandleWidth)
    val candleSpacing = config.candleSpacing
    val indicatorCache = remember { KlineIndicatorSeriesCache() }
    val builtInIndicatorDefinitions = remember(config) { config.builtInIndicators() }
    val legacyCustomIndicatorDefinitions = remember(customIndicators) {
        customIndicators.map { it.asIndicator() }
    }
    val indicatorDefinitions = remember(builtInIndicatorDefinitions, indicators, legacyCustomIndicatorDefinitions) {
        builtInIndicatorDefinitions + indicators + legacyCustomIndicatorDefinitions
    }
    val standaloneExternalSubIndicatorIds = remember(indicators) {
        indicators
            .filter { it.pane == KlineIndicatorPane.Sub && it.overlayId == null }
            .map { it.id }
            .toSet()
    }
    val indicatorSeries by produceState(
        initialValue = emptyList<KlineIndicatorSeries>(),
        candles,
        indicatorDefinitions,
    ) {
        value = withContext(Dispatchers.Default) {
            computeKlineIndicatorSeries(indicatorDefinitions, candles, indicatorCache)
        }
    }

    var previousCount by remember { mutableStateOf(0) }
    var previousFirstTimestamp by remember { mutableStateOf<Long?>(null) }
    var previousLastTimestamp by remember { mutableStateOf<Long?>(null) }
    var visibleMin by remember { mutableStateOf(0.0) }
    var visibleMax by remember { mutableStateOf(100.0) }
    var macdRange by remember { mutableStateOf(0.0 to 0.0) }
    var latestVisiblePrice by remember { mutableStateOf<Double?>(null) }
    val candleTimestamps = remember(candles) { candles.map { it.timestamp } }
    var timeAxisAnchorSet by remember { mutableStateOf<TimeAxisAnchorSet?>(null) }
    var anchorCandleWidth by remember { mutableStateOf(0f) }
    var anchorCandleSpacing by remember { mutableStateOf(0f) }
    var anchorCanvasWidth by remember { mutableStateOf(0f) }
    var anchorLabelCount by remember { mutableStateOf(0) }
    val entranceProgress = remember { Animatable(if (config.entranceAnimation) 0f else 1f) }
    var hasPlayedEntranceAnimation by remember { mutableStateOf(!config.entranceAnimation) }

    val scrollBounds = remember(candles.size, candleWidth, candleSpacing, gestureState.canvasSize.width) {
        if (gestureState.canvasSize == Size.Zero) 0f to 0f
        else calculateScrollBounds(candles.size, candleWidth, candleSpacing, gestureState.canvasSize.width)
    }
    val timeTicks = remember(
        timeAxisAnchorSet,
        candleTimestamps,
        candleWidth,
        candleSpacing,
        gestureState.xOffset,
        gestureState.canvasSize.width,
        config.timeAxisLabelCount,
        config.timeAxisFadeAnimation,
    ) {
        projectTimeAxisTicks(
            anchorSet = timeAxisAnchorSet,
            candleTimestamps = candleTimestamps,
            candleWidth = candleWidth,
            candleSpacing = candleSpacing,
            xOffset = gestureState.xOffset,
            canvasWidth = gestureState.canvasSize.width,
            labelCount = config.timeAxisLabelCount,
            fadeAnimation = config.timeAxisFadeAnimation,
        )
    }
    val currentFirstTimestamp = candles.firstOrNull()?.timestamp
    val currentLastTimestamp = candles.lastOrNull()?.timestamp
    val isHistoricalPrepend = previousCount > 0 &&
        candles.size > previousCount &&
        previousLastTimestamp != null &&
        currentLastTimestamp == previousLastTimestamp
    val isAppend = previousCount > 0 &&
        candles.size > previousCount &&
        currentFirstTimestamp == previousFirstTimestamp
    val isReplacement = previousCount > 0 &&
        !isHistoricalPrepend &&
        !isAppend &&
        (currentFirstTimestamp != previousFirstTimestamp || currentLastTimestamp != previousLastTimestamp)

    SideEffect {
        if (isReplacement) {
            gestureState.resetInitialPosition()
        }
        if (isHistoricalPrepend) {
            gestureState.markHistoricalDataLoaded()
        }
        val prependedCount = if (isHistoricalPrepend) candles.size - previousCount else 0
        if (prependedCount > 0) {
            timeAxisAnchorSet = timeAxisAnchorSet?.let { anchor ->
                anchor.copy(originIndex = anchor.originIndex + prependedCount)
            }
        }
        gestureState.syncViewport(
            candleCount = candles.size,
            candleWidth = candleWidth,
            candleSpacing = candleSpacing,
            leftBound = scrollBounds.first,
            rightBound = scrollBounds.second,
        )
        val canvasWidth = gestureState.canvasSize.width
        val shouldRecalculateTimeAnchors = timeAxisAnchorSet == null ||
            isReplacement ||
            candleWidth != anchorCandleWidth ||
            candleSpacing != anchorCandleSpacing ||
            canvasWidth != anchorCanvasWidth ||
            config.timeAxisLabelCount != anchorLabelCount
        if (shouldRecalculateTimeAnchors) {
            timeAxisAnchorSet = calculateTimeAxisAnchorSet(
                candleTimestamps = candleTimestamps,
                candleWidth = candleWidth,
                candleSpacing = candleSpacing,
                xOffset = gestureState.xOffset,
                canvasWidth = canvasWidth,
                labelCount = config.timeAxisLabelCount,
            )
            anchorCandleWidth = candleWidth
            anchorCandleSpacing = candleSpacing
            anchorCanvasWidth = canvasWidth
            anchorLabelCount = config.timeAxisLabelCount
        }
        state.zoom = gestureState.zoom
        state.xOffset = gestureState.xOffset
        state.canvasSize = gestureState.canvasSize
        state.visiblePriceRange = visibleMin..visibleMax
        if (!gestureState.showCrosshair) {
            state.crosshair = null
        }
        previousCount = candles.size
        previousFirstTimestamp = currentFirstTimestamp
        previousLastTimestamp = currentLastTimestamp
    }

    LaunchedEffect(config.entranceAnimation, candles.isNotEmpty()) {
        when {
            !config.entranceAnimation -> {
                entranceProgress.snapTo(1f)
                hasPlayedEntranceAnimation = true
            }
            candles.isEmpty() -> {
                entranceProgress.snapTo(0f)
                hasPlayedEntranceAnimation = false
            }
            !hasPlayedEntranceAnimation -> {
                entranceProgress.snapTo(0f)
                entranceProgress.animateTo(1f, tween(durationMillis = 900))
                hasPlayedEntranceAnimation = true
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Column(
            Modifier
                .fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .onGloballyPositioned { coordinates ->
                        gestureState.updateArea(
                            ChartGestureState.MainAreaId,
                            coordinates.positionInParent().y,
                            coordinates.size.height.toFloat(),
                        )
                    }
                    .klineGestures(
                        areaId = ChartGestureState.MainAreaId,
                        candles = candles,
                        config = config,
                        gestureState = gestureState,
                        scrollBounds = scrollBounds,
                        onTap = { areaId, position ->
                            if (areaId != ChartGestureState.MainAreaId) return@klineGestures false
                            val overlayHit = hitTestOverlayLine(
                                lines = overlayLines,
                                minPrice = visibleMin,
                                maxPrice = visibleMax,
                                canvasHeight = gestureState.areaHeight(ChartGestureState.MainAreaId),
                                tapY = position.y,
                            )
                            if (overlayHit != null) {
                                onOverlayLineClick(overlayHit)
                                return@klineGestures true
                            }

                            val markerHit = hitTestHistoryMarker(
                                markers = historyMarkers,
                                candles = candles,
                                candleWidth = candleWidth,
                                candleSpacing = candleSpacing,
                                xOffset = gestureState.xOffset,
                                minPrice = visibleMin,
                                maxPrice = visibleMax,
                                canvasHeight = gestureState.areaHeight(ChartGestureState.MainAreaId),
                                tapX = position.x,
                                tapY = position.y,
                                config = config.markerStyle.toHitTestConfig(),
                            )
                            if (markerHit != null) {
                                onHistoryMarkerClick(markerHit)
                                return@klineGestures true
                            }
                            false
                        },
                    ),
            ) {
                MainChartCanvas(
                    candles = candles,
                    config = config,
                    colors = colors,
                    historyMarkers = historyMarkers,
                    indicatorSeries = indicatorSeries,
                    entranceProgress = entranceProgress.value,
                    gestureState = gestureState,
                    candleWidth = candleWidth,
                    candleSpacing = candleSpacing,
                    timeTicks = timeTicks,
                    onRangeChanged = { minValue, maxValue ->
                        visibleMin = minValue
                        visibleMax = maxValue
                        latestVisiblePrice = when (config.lastPriceMode) {
                            LastPriceMode.Latest -> candles.lastOrNull()?.close
                            LastPriceMode.RightmostVisible -> {
                                if (candles.isEmpty()) {
                                    null
                                } else {
                                    val step = candleWidth + candleSpacing
                                    val endIndex = ((gestureState.canvasSize.width - gestureState.xOffset) / step)
                                        .toInt()
                                        .coerceIn(0, candles.lastIndex)
                                    candles[endIndex].close
                                }
                            }
                        }
                    },
                )

                MainIndicatorLabels(
                    candles = candles,
                    config = config,
                    colors = colors,
                    indicatorSeries = indicatorSeries,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                )
                if (config.showPriceAxis) {
                    PriceAxisLabels(
                        minPrice = visibleMin,
                        maxPrice = visibleMax,
                        tickCount = config.priceAxisTickCount,
                        precision = config.pricePrecision,
                        colors = colors,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(config.priceAxisWidth)
                            .fillMaxSize(),
                    )
                    if (config.showLatestPriceLabel && candles.isNotEmpty()) {
                        LatestPriceLabel(
                            price = latestVisiblePrice ?: candles.last().close,
                            minPrice = visibleMin,
                            maxPrice = visibleMax,
                            precision = config.pricePrecision,
                            colors = colors,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .width(config.priceAxisWidth)
                                .fillMaxSize(),
                        )
                    }
                    if (candles.isNotEmpty()) {
                        val mainVisibleRange = visibleIndexRange(
                            dataSize = candles.size,
                            candleWidth = candleWidth,
                            candleSpacing = candleSpacing,
                            xOffset = gestureState.xOffset,
                            canvasWidth = gestureState.canvasSize.width,
                        )
                        var indicatorColorIndex = 0
                        indicatorSeries
                            .filter { it.pane == KlineIndicatorPane.Main }
                            .forEach { series ->
                                if (series.showLatestValue) {
                                    val range = when (series.scaleMode) {
                                        KlineIndicatorScaleMode.SharedPriceAxis -> visibleMin to visibleMax
                                        KlineIndicatorScaleMode.IndependentAxis -> visibleMinMax(
                                            candleWidth,
                                            candleSpacing,
                                            gestureState.xOffset,
                                            gestureState.canvasSize.width,
                                            *series.lines.map { it.values }.toTypedArray(),
                                        )
                                    }
                                    series.lines.forEachIndexed { lineIndex, line ->
                                        val latest = line.latestVisibleValue(
                                            mode = config.lastPriceMode,
                                            visibleRange = mainVisibleRange,
                                            lastIndex = candles.lastIndex,
                                        ) ?: return@forEachIndexed
                                        LatestPriceLabel(
                                            price = latest.value,
                                            minPrice = range.first,
                                            maxPrice = range.second,
                                            precision = config.pricePrecision,
                                            colors = colors,
                                            color = colors.indicatorColor(indicatorColorIndex + lineIndex),
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .width(config.priceAxisWidth)
                                                .fillMaxSize(),
                                        )
                                    }
                                }
                                indicatorColorIndex += series.lines.size
                            }
                    }
                }
                OverlayLineLayer(
                    lines = overlayLines,
                    minPrice = visibleMin,
                    maxPrice = visibleMax,
                    style = overlayStyle,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (config.showVolume) {
                ChartPanel(
                    areaId = "VOL",
                    gestureState = gestureState,
                    config = config,
                    candles = candles,
                    candleWidth = candleWidth,
                    candleSpacing = candleSpacing,
                    scrollBounds = scrollBounds,
                    height = config.volumeHeight,
                ) {
                    VolumePanel(
                        candles = candles,
                        config = config,
                        colors = colors,
                        indicatorSeries = indicatorSeries,
                        timeTicks = timeTicks,
                        candleWidth = candleWidth,
                        candleSpacing = candleSpacing,
                        xOffset = gestureState.xOffset,
                    )
                }
            }

            config.subIndicators.forEach { indicator ->
                ChartPanel(
                    areaId = indicator.name,
                    gestureState = gestureState,
                    config = config,
                    candles = candles,
                    candleWidth = candleWidth,
                    candleSpacing = candleSpacing,
                    scrollBounds = scrollBounds,
                    height = config.indicatorHeight,
                ) {
                    SubIndicatorPanel(
                        indicator = indicator,
                        config = config,
                        colors = colors,
                        indicatorSeries = indicatorSeries,
                        timeTicks = timeTicks,
                        candleWidth = candleWidth,
                        candleSpacing = candleSpacing,
                        xOffset = gestureState.xOffset,
                        onMacdRangeChanged = { macdRange = it },
                    )
                }
            }

            customIndicators.forEach { indicator ->
                ChartPanel(
                    areaId = indicator.id,
                    gestureState = gestureState,
                    config = config,
                    candles = candles,
                    candleWidth = candleWidth,
                    candleSpacing = candleSpacing,
                    scrollBounds = scrollBounds,
                    height = config.indicatorHeight,
                ) {
                    CustomIndicatorPanel(
                        indicator = indicator,
                        candles = candles,
                        colors = colors,
                        values = indicatorSeries.firstOrNull { it.id == indicator.id }?.toIndicatorValues(),
                        timeTicks = timeTicks,
                        candleWidth = candleWidth,
                        candleSpacing = candleSpacing,
                        xOffset = gestureState.xOffset,
                    )
                }
            }

            indicatorSeries
                .filter { it.pane == KlineIndicatorPane.Sub && it.overlayId == null && it.id in standaloneExternalSubIndicatorIds }
                .forEach { series ->
                    ChartPanel(
                        areaId = series.id,
                        gestureState = gestureState,
                        config = config,
                        candles = candles,
                        candleWidth = candleWidth,
                        candleSpacing = candleSpacing,
                        scrollBounds = scrollBounds,
                        height = series.height ?: config.indicatorHeight,
                    ) {
                        IndicatorSeriesPanel(
                            series = series,
                            colors = colors,
                            timeTicks = timeTicks,
                            candleWidth = candleWidth,
                            candleSpacing = candleSpacing,
                            xOffset = gestureState.xOffset,
                        )
                    }
                }

            customPanels.forEach { panel ->
                ChartPanel(
                    areaId = panel.id,
                    gestureState = gestureState,
                    config = config,
                    candles = candles,
                    candleWidth = candleWidth,
                    candleSpacing = candleSpacing,
                    scrollBounds = scrollBounds,
                    height = panel.height,
                ) {
                    panel.content(
                        KlinePanelScope(
                            candles = candles,
                            state = state,
                            candleWidth = candleWidth,
                            candleSpacing = candleSpacing,
                            xOffset = gestureState.xOffset,
                        )
                    )
                }
            }

            if (config.showTimeAxis) {
                TimeAxis(
                    candles = candles,
                    config = config,
                    timeTicks = timeTicks,
                    colors = colors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(config.timeAxisHeight),
                )
            }
        }

        overlayContent(
            KlineChartOverlayScope(
                candles = candles,
                state = state,
                candleWidth = candleWidth,
                candleSpacing = candleSpacing,
                visibleMinPrice = visibleMin,
                visibleMaxPrice = visibleMax,
                chartHeight = gestureState.areaHeight(ChartGestureState.MainAreaId),
            )
        )

        if (gestureState.showCrosshair && candles.isNotEmpty()) {
            val nearestIndex = gestureState.nearestIndex(
                gestureState.crosshairPosition.x,
                candles.size,
                candleWidth,
                candleSpacing,
            )
            CrosshairOverlay(
                candles = candles,
                config = config,
                colors = colors,
                publicState = state,
                gestureState = gestureState,
                candleWidth = candleWidth,
                candleSpacing = candleSpacing,
                visibleMin = visibleMin,
                visibleMax = visibleMax,
                macdRange = macdRange,
            )
            if (nearestIndex in candles.indices && (config.showCrosshairInfoPanel || crosshairInfoContent != null)) {
                val crosshairX = nearestIndex * (candleWidth + candleSpacing) +
                    gestureState.xOffset +
                    candleWidth / 2
                val infoScope = KlineCrosshairInfoScope(
                    candle = candles[nearestIndex],
                    candleIndex = nearestIndex,
                    candles = candles,
                    historyMarkers = historyMarkers,
                    state = state,
                    crosshairX = crosshairX,
                    canvasWidth = gestureState.canvasSize.width,
                    config = config,
                )
                if (crosshairInfoContent != null) {
                    crosshairInfoContent(infoScope)
                } else {
                    CrosshairInfoPanel(
                        candle = candles[nearestIndex],
                        canvasWidth = gestureState.canvasSize.width,
                        crosshairX = crosshairX,
                        config = config,
                        colors = colors,
                        historyMarkers = historyMarkers,
                    )
                }
            }
        }
    }
}
