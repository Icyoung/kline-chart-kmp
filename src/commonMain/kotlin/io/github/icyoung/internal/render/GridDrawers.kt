package io.github.icyoung.internal.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import io.github.icyoung.KlineChartColors
import io.github.icyoung.internal.axis.TimeAxisTick

internal fun DrawScope.drawGrid(color: Color, width: Float, height: Float, timeTicks: List<TimeAxisTick> = emptyList()) {
    val stroke = 0.5.dp.toPx()
    repeat(6) { index ->
        val y = height * index / 5f
        drawLine(color, Offset(0f, y), Offset(width, y), stroke)
    }
    drawTimeGridLines(color, height, timeTicks)
}

internal fun DrawScope.drawTimeGridLines(color: Color, height: Float, timeTicks: List<TimeAxisTick>) {
    val stroke = 0.5.dp.toPx()
    timeTicks.forEach { tick ->
        drawLine(
            color = color,
            start = Offset(tick.x, 0f),
            end = Offset(tick.x, height),
            strokeWidth = stroke,
        )
    }
}

internal fun DrawScope.drawPriceAxis(left: Float, height: Float, colors: KlineChartColors) {
    repeat(6) { index ->
        val y = height * index / 5f
        drawLine(colors.grid, Offset(left, y), Offset(left + 4.dp.toPx(), y), 0.5.dp.toPx())
    }
}
