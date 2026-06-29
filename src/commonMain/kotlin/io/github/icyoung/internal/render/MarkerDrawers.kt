package io.github.icyoung.internal.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import io.github.icyoung.KlineChartColors
import io.github.icyoung.KlineHistoryMarker
import io.github.icyoung.KlineMarkerPlacement
import io.github.icyoung.KlineMarkerStyle
import io.github.icyoung.core.calculateMarkerLayouts
import io.github.icyoung.model.OhlcvCandle
import kotlin.math.max

@OptIn(ExperimentalTextApi::class)
internal fun DrawScope.drawHistoryMarkers(
    markers: List<KlineHistoryMarker>,
    candles: List<OhlcvCandle>,
    candleWidth: Float,
    candleSpacing: Float,
    xOffset: Float,
    minPrice: Double,
    maxPrice: Double,
    textMeasurer: TextMeasurer,
    style: TextStyle,
    colors: KlineChartColors,
    markerStyle: KlineMarkerStyle,
) {
    if (markers.isEmpty() || candles.isEmpty() || maxPrice <= minPrice) return
    val layouts = calculateMarkerLayouts(
        markers = markers,
        candles = candles,
        candleWidth = candleWidth,
        candleSpacing = candleSpacing,
        xOffset = xOffset,
        minPrice = minPrice,
        maxPrice = maxPrice,
        canvasHeight = size.height,
        style = markerStyle,
    )
    layouts.forEach { layoutInfo ->
        val centerX = layoutInfo.centerX
        if (centerX < -candleWidth || centerX > size.width + candleWidth) return@forEach
        val isBelow = layoutInfo.placement == KlineMarkerPlacement.Below
        val baseY = layoutInfo.centerY
        if (baseY.isNaN() || baseY < -100f || baseY > size.height + 100f) return@forEach
        val layout = textMeasurer.measure(layoutInfo.label, style.copy(color = colors.labelText))
        val padding = 5f
        val squareSize = max(layout.size.width.toFloat(), layout.size.height.toFloat()) + padding * 2
        if (!squareSize.isFinite() || squareSize <= 0f) return@forEach
        val background = if (isBelow) colors.rising else colors.falling
        drawRoundRect(
            color = background,
            topLeft = Offset(centerX - squareSize / 2, baseY - squareSize / 2),
            size = Size(squareSize, squareSize),
            cornerRadius = CornerRadius(8f, 8f),
        )
        val triangleHeight = 8f
        val triangleBase = 12f
        val triangle = Path().apply {
            if (isBelow) {
                moveTo(centerX, baseY - squareSize / 2 - triangleHeight)
                lineTo(centerX - triangleBase / 2, baseY - squareSize / 2)
                lineTo(centerX + triangleBase / 2, baseY - squareSize / 2)
            } else {
                moveTo(centerX, baseY + squareSize / 2 + triangleHeight)
                lineTo(centerX - triangleBase / 2, baseY + squareSize / 2)
                lineTo(centerX + triangleBase / 2, baseY + squareSize / 2)
            }
            close()
        }
        drawPath(triangle, background)
        drawText(
            textLayoutResult = layout,
            topLeft = Offset(centerX - layout.size.width / 2, baseY - layout.size.height / 2),
        )
    }
}
