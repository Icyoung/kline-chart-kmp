package io.github.icyoung.internal.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.Dp
import io.github.icyoung.KlineChartConfig
import io.github.icyoung.internal.gesture.ChartGestureState
import io.github.icyoung.internal.gesture.klineGestures
import io.github.icyoung.model.OhlcvCandle

@Composable
internal fun ChartPanel(
    areaId: String,
    gestureState: ChartGestureState,
    config: KlineChartConfig,
    candles: List<OhlcvCandle>,
    candleWidth: Float,
    candleSpacing: Float,
    scrollBounds: Pair<Float, Float>,
    height: Dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .onGloballyPositioned { coordinates ->
                gestureState.updateArea(
                    areaId = areaId,
                    topY = coordinates.positionInParent().y,
                    height = coordinates.size.height.toFloat(),
                )
            }
            .klineGestures(
                areaId = areaId,
                candles = candles,
                config = config,
                gestureState = gestureState,
                scrollBounds = scrollBounds,
            ),
    ) {
        content()
    }
}
