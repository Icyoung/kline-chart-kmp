package io.github.icyoung.internal.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import io.github.icyoung.KlineChartColors

internal fun DrawScope.drawGrid(color: Color, width: Float, height: Float) {
    val stroke = 0.5.dp.toPx()
    repeat(6) { index ->
        val y = height * index / 5f
        drawLine(color, Offset(0f, y), Offset(width, y), stroke)
    }
    repeat(5) { index ->
        val x = width * index / 4f
        drawLine(color, Offset(x, 0f), Offset(x, height), stroke)
    }
}

internal fun DrawScope.drawPriceAxis(left: Float, height: Float, colors: KlineChartColors) {
    repeat(6) { index ->
        val y = height * index / 5f
        drawLine(colors.grid, Offset(left, y), Offset(left + 4.dp.toPx(), y), 0.5.dp.toPx())
    }
}
