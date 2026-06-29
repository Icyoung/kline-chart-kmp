package io.github.icyoung

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class KlineOverlayLine(
    val id: String,
    val price: Double,
    val label: String,
    val color: Color,
    val labelTextColor: Color = Color.White,
    val labelBackgroundColor: Color = color,
    val labelAlign: KlineOverlayLabelAlign = KlineOverlayLabelAlign.End,
    val lineStyle: KlineOverlayLineStyle = KlineOverlayLineStyle.Dashed,
    val extendLine: Boolean = true,
)

enum class KlineOverlayLabelAlign {
    Start,
    End,
}

enum class KlineOverlayLineStyle {
    Solid,
    Dashed,
}

data class KlineOverlayStyle(
    val labelHeight: Dp = 20.dp,
    val horizontalPadding: Dp = 8.dp,
    val lineWidth: Dp = 1.dp,
)

data class KlineOverlayLineHit(
    val id: String,
    val line: KlineOverlayLine,
)
