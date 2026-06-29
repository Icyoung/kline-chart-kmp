package io.github.icyoung.internal.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import io.github.icyoung.KlineOverlayLabelAlign
import io.github.icyoung.KlineOverlayLine
import io.github.icyoung.KlineOverlayLineStyle
import io.github.icyoung.KlineOverlayStyle
import io.github.icyoung.core.priceToY

private val OverlayDashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))

@OptIn(ExperimentalTextApi::class)
@Composable
internal fun OverlayLineLayer(
    lines: List<KlineOverlayLine>,
    minPrice: Double,
    maxPrice: Double,
    style: KlineOverlayStyle,
    modifier: Modifier = Modifier,
) {
    if (lines.isEmpty() || maxPrice <= minPrice) return
    val textMeasurer = rememberTextMeasurer()
    val textStyle = MaterialTheme.typography.labelSmall

    Canvas(modifier.fillMaxSize()) {
        lines.forEach { line ->
            val y = priceToY(line.price, minPrice, maxPrice, size.height)
            if (y < 0f || y > size.height) return@forEach
            val pathEffect = if (line.lineStyle == KlineOverlayLineStyle.Dashed) OverlayDashEffect else null
            if (line.extendLine) {
                drawLine(
                    color = line.color,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = style.lineWidth.toPx(),
                    pathEffect = pathEffect,
                )
            }

            val layout = textMeasurer.measure(line.label, textStyle.copy(color = line.labelTextColor))
            val paddingX = style.horizontalPadding.toPx()
            val labelHeight = style.labelHeight.toPx()
            val labelWidth = layout.size.width + paddingX * 2
            val maxTop = (size.height - labelHeight).coerceAtLeast(0f)
            val top = (y - labelHeight / 2f).coerceIn(0f, maxTop)
            val left = when (line.labelAlign) {
                KlineOverlayLabelAlign.Start -> 0f
                KlineOverlayLabelAlign.End -> size.width - labelWidth
            }
            drawRoundRect(
                color = line.labelBackgroundColor,
                topLeft = Offset(left, top),
                size = Size(labelWidth, labelHeight),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            )
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(left + paddingX, top + (labelHeight - layout.size.height) / 2f),
            )
        }
    }
}
