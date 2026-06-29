package io.github.icyoung.internal.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.icyoung.CrosshairLabelContext
import io.github.icyoung.CrosshairState
import io.github.icyoung.KlineChartColors
import io.github.icyoung.KlineChartConfig
import io.github.icyoung.KlineChartState
import io.github.icyoung.SubIndicator
import io.github.icyoung.internal.gesture.ChartGestureState
import io.github.icyoung.internal.render.ChartDashEffect
import io.github.icyoung.internal.render.formatCompact
import io.github.icyoung.internal.render.visibleMax
import io.github.icyoung.model.OhlcvCandle
import io.github.icyoung.util.formatChartValue

@Composable
internal fun CrosshairOverlay(
    candles: List<OhlcvCandle>,
    config: KlineChartConfig,
    colors: KlineChartColors,
    publicState: KlineChartState,
    gestureState: ChartGestureState,
    candleWidth: Float,
    candleSpacing: Float,
    visibleMin: Double,
    visibleMax: Double,
    macdRange: Pair<Double, Double>,
) {
    val areaId = gestureState.crosshairAreaId
    val areaTop = gestureState.areaTop(areaId)
    val areaHeight = gestureState.areaHeight(areaId)
    val localY = (gestureState.crosshairAbsoluteY - areaTop).coerceIn(0f, areaHeight)
    val yFraction = if (areaHeight > 0f) (localY / areaHeight).coerceIn(0f, 1f) else 0f
    val nearest = gestureState.nearestIndex(gestureState.crosshairPosition.x, candles.size, candleWidth, candleSpacing)
    val customLabel = config.crosshairLabelFormatter?.invoke(
        CrosshairLabelContext(
            areaId = areaId,
            yFraction = yFraction,
            candleIndex = nearest.takeIf { it in candles.indices },
            candle = candles.getOrNull(nearest),
            visibleMinPrice = visibleMin,
            visibleMaxPrice = visibleMax,
            macdRange = macdRange.first..macdRange.second,
        )
    )
    val label = customLabel ?: defaultCrosshairLabel(
        areaId = areaId,
        yFraction = yFraction,
        candles = candles,
        config = config,
        gestureState = gestureState,
        candleWidth = candleWidth,
        candleSpacing = candleSpacing,
        visibleMin = visibleMin,
        visibleMax = visibleMax,
        macdRange = macdRange,
    )

    SideEffect {
        publicState.crosshair = if (nearest in candles.indices) {
            CrosshairState(
                candleIndex = nearest,
                candle = candles[nearest],
                position = gestureState.crosshairPosition,
                absoluteY = gestureState.crosshairAbsoluteY,
                areaId = areaId,
                label = label,
            )
        } else {
            null
        }
    }

    Canvas(Modifier.fillMaxSize()) {
        if (nearest in candles.indices) {
            val x = nearest * (candleWidth + candleSpacing) + gestureState.xOffset + candleWidth / 2
            drawLine(
                color = colors.crosshair,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 0.5.dp.toPx(),
                pathEffect = ChartDashEffect,
            )
            drawLine(
                color = colors.crosshair,
                start = Offset(0f, gestureState.crosshairAbsoluteY),
                end = Offset(size.width, gestureState.crosshairAbsoluteY),
                strokeWidth = 0.5.dp.toPx(),
                pathEffect = ChartDashEffect,
            )
            drawCircle(colors.crosshair, radius = 3.dp.toPx(), center = Offset(x, gestureState.crosshairAbsoluteY))
        }
    }

    CrosshairYLabel(label, gestureState.crosshairAbsoluteY, colors)
}

private fun defaultCrosshairLabel(
    areaId: String,
    yFraction: Float,
    candles: List<OhlcvCandle>,
    config: KlineChartConfig,
    gestureState: ChartGestureState,
    candleWidth: Float,
    candleSpacing: Float,
    visibleMin: Double,
    visibleMax: Double,
    macdRange: Pair<Double, Double>,
): String? {
    return when (areaId) {
        ChartGestureState.MainAreaId -> (visibleMin + (1.0 - yFraction) * (visibleMax - visibleMin))
            .formatChartValue(config.pricePrecision)
        "VOL" -> {
            val maxVol = visibleMax(candles, candleWidth, candleSpacing, gestureState.xOffset, gestureState.canvasSize.width) {
                it.volume
            }
            (maxVol * (1f - yFraction)).formatCompact()
        }
        SubIndicator.RSI.name, SubIndicator.KDJ.name -> ((1f - yFraction) * 100f).toDouble().formatChartValue(1)
        SubIndicator.MACD.name -> {
            val (minValue, maxValue) = macdRange
            if (maxValue > minValue) {
                (maxValue - yFraction * (maxValue - minValue)).formatChartValue(4)
            } else {
                null
            }
        }
        else -> null
    }
}

@Composable
private fun CrosshairYLabel(label: String?, absoluteY: Float, colors: KlineChartColors) {
    if (label == null) return

    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
        val labelHeight = 17.dp
        with(LocalDensity.current) {
            val maxYOffset = (maxHeight.toPx() - labelHeight.toPx()).coerceAtLeast(0f)
            val y = (absoluteY - labelHeight.toPx() / 2f).coerceIn(0f, maxYOffset).toDp()
            Box(
                Modifier
                    .offset(y = y)
                    .padding(end = 2.dp)
                    .background(colors.labelBackground, RoundedCornerShape(2.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = colors.labelText, fontSize = 10.sp, lineHeight = 10.sp)
            }
        }
    }
}
