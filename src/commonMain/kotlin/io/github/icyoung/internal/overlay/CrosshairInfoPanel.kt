package io.github.icyoung.internal.overlay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.icyoung.KlineChartColors
import io.github.icyoung.KlineChartConfig
import io.github.icyoung.KlineHistoryMarker
import io.github.icyoung.core.markersForCandle
import io.github.icyoung.internal.render.formatCompact
import io.github.icyoung.model.OhlcvCandle
import io.github.icyoung.util.formatChartValue

@Composable
internal fun CrosshairInfoPanel(
    candle: OhlcvCandle,
    canvasWidth: Float,
    crosshairX: Float,
    config: KlineChartConfig,
    colors: KlineChartColors,
    historyMarkers: List<KlineHistoryMarker>,
    modifier: Modifier = Modifier,
) {
    val priceChange = candle.close - candle.open
    val priceChangePercent = if (candle.open != 0.0) priceChange / candle.open * 100.0 else 0.0
    val changeColor = when {
        priceChange > 0.0 -> colors.rising
        priceChange < 0.0 -> colors.falling
        else -> colors.textSecondary
    }
    val alignment = if (crosshairX < canvasWidth / 2f) Alignment.TopEnd else Alignment.TopStart
    val markers = markersForCandle(candle, historyMarkers)

    Box(Modifier.fillMaxSize().then(modifier)) {
        Card(
            modifier = Modifier
                .align(alignment)
                .padding(
                    start = 8.dp,
                    top = 50.dp,
                    end = if (alignment == Alignment.TopEnd) config.priceAxisWidth else 8.dp,
                )
                .wrapContentWidth()
                .widthIn(min = 128.dp, max = 168.dp),
            colors = CardDefaults.cardColors(containerColor = colors.background),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(4.dp),
        ) {
            Column(
                modifier = Modifier.padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                InfoRow("Time", candle.timestamp.let(config.timeLabelFormatter), colors.textPrimary, colors)
                InfoRow("Open", candle.open.formatChartValue(config.pricePrecision), colors.textPrimary, colors)
                InfoRow("High", candle.high.formatChartValue(config.pricePrecision), colors.rising, colors)
                InfoRow("Low", candle.low.formatChartValue(config.pricePrecision), colors.falling, colors)
                InfoRow("Close", candle.close.formatChartValue(config.pricePrecision), changeColor, colors)
                if (priceChange != 0.0) {
                    val prefix = if (priceChangePercent > 0) "+" else ""
                    InfoRow("Change", "$prefix${priceChangePercent.formatChartValue(2)}%", changeColor, colors)
                }
                if (candle.volume > 0.0) {
                    InfoRow("Volume", candle.volume.formatCompact(), colors.textSecondary, colors)
                }
                if (markers.isNotEmpty()) {
                    InfoRow("Markers", markers.size.toString(), colors.textSecondary, colors)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: Color, colors: KlineChartColors) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
        Text(
            text = value,
            color = valueColor,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.End,
        )
    }
}
