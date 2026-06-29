package io.github.icyoung

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import io.github.icyoung.model.OhlcvCandle

data class KlinePanelSpec(
    val id: String,
    val height: Dp,
    val content: @Composable KlinePanelScope.() -> Unit,
)

data class KlinePanelScope(
    val candles: List<OhlcvCandle>,
    val state: KlineChartState,
    val candleWidth: Float,
    val candleSpacing: Float,
    val xOffset: Float,
)
