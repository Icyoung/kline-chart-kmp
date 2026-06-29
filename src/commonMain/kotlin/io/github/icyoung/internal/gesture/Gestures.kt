package io.github.icyoung.internal.gesture

import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import io.github.icyoung.KlineChartConfig
import io.github.icyoung.core.calculateScrollBounds
import io.github.icyoung.model.OhlcvCandle

internal fun Modifier.klineGestures(
    areaId: String,
    candles: List<OhlcvCandle>,
    config: KlineChartConfig,
    gestureState: ChartGestureState,
    scrollBounds: Pair<Float, Float>,
    onTap: (areaId: String, position: Offset) -> Boolean = { _, _ -> false },
): Modifier {
    return this
        .pointerInput(candles.size, gestureState.canvasSize, gestureState.zoom, config.baseCandleWidth, config.candleSpacing) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    val zoomChange = event.calculateZoom()
                    if (event.changes.size > 1 && zoomChange != 1f) {
                        gestureState.handleZoom(
                            zoomFactor = zoomChange,
                            baseCandleWidth = config.baseCandleWidth,
                            baseCandleSpacing = config.candleSpacing,
                            candleCount = candles.size,
                            focusX = event.calculateCentroid(useCurrent = true).x,
                            minCandleWidth = config.minCandleWidth,
                            maxCandleWidth = config.maxCandleWidth,
                            calculateBounds = { width, spacing, canvasWidth ->
                                calculateScrollBounds(candles.size, width, spacing, canvasWidth)
                            },
                        )
                        event.changes.forEach { if (it.pressed) it.consume() }
                    }
                }
            }
        }
        .pointerInput(candles.size, gestureState.canvasSize, gestureState.zoom, scrollBounds) {
            with(gestureState) {
                handleGestures(areaId, { scrollBounds }, onTap)
            }
        }
}
