package io.github.icyoung.internal.layout

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
    var entered by remember(areaId) { mutableStateOf(!config.panelAnimation) }
    LaunchedEffect(areaId, config.panelAnimation) {
        entered = true
    }
    val contentProgress by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = if (config.panelAnimation) 220 else 0),
        label = "chart-panel-content",
    )
    val density = LocalDensity.current
    val enterOffset = with(density) { 10.dp.toPx() }

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    alpha = contentProgress,
                    translationY = (1f - contentProgress) * enterOffset,
                ),
        ) {
            content()
        }
    }
}
