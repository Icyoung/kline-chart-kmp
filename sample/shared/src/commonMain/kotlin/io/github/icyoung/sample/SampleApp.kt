package io.github.icyoung.sample

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.icyoung.ChartStyle
import io.github.icyoung.KlineBuiltInIndicatorSpecs
import io.github.icyoung.KlineChart
import io.github.icyoung.KlineChartColors
import io.github.icyoung.KlineChartConfig
import io.github.icyoung.KlineCustomIndicator
import io.github.icyoung.KlineHistoryMarker
import io.github.icyoung.KlineIndicatorValues
import io.github.icyoung.KlineMarkerPlacement
import io.github.icyoung.MainIndicator
import io.github.icyoung.SubIndicator
import io.github.icyoung.model.OhlcvCandle
import io.github.icyoung.rememberKlineChartDataState
import io.github.icyoung.sample.data.MarketInstrument
import io.github.icyoung.sample.data.BinanceMarketDataSource
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private val SampleBars = listOf("1m", "5m", "15m", "1h", "4h", "1d")
private const val VwapIndicatorId = "VWAP"
private val MainIndicatorTabs = KlineBuiltInIndicatorSpecs.mainIndicators.map { it.id }.toSet()
private val SubIndicatorTabs = listOf(KlineBuiltInIndicatorSpecs.VOL.id) +
    KlineBuiltInIndicatorSpecs.subIndicators.filter { it.id != KlineBuiltInIndicatorSpecs.VOL.id }.map { it.id } +
    VwapIndicatorId
private val IndicatorTabs = MainIndicatorTabs.toList() + SubIndicatorTabs

@Composable
fun SampleApp() {
    val dataSource = remember { BinanceMarketDataSource() }
    val scope = rememberCoroutineScope()
    var instruments by remember { mutableStateOf<List<MarketInstrument>>(emptyList()) }
    var selectedInstrumentId by remember { mutableStateOf("BTCUSDT") }
    var selectedBar by remember { mutableStateOf("1m") }
    val dataState = rememberKlineChartDataState(sampleCandles())
    val candles = dataState.candles
    var isLoading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Sample data") }
    var selectedMainIndicators by remember { mutableStateOf(setOf("MA")) }
    var selectedSubIndicators by remember { mutableStateOf(setOf("VOL")) }

    fun loadCandles(instrumentId: String, bar: String) {
        scope.launch {
            isLoading = true
            status = "Loading $instrumentId $bar"
            runCatching {
                dataSource.candles(instrumentId = instrumentId, bar = bar, limit = 180)
            }.onSuccess { remoteCandles ->
                dataState.replaceAll(remoteCandles.ifEmpty { sampleCandles() })
                status = if (remoteCandles.isEmpty()) "Sample data" else "Binance $instrumentId $bar"
            }.onFailure { error ->
                dataState.replaceAll(sampleCandles())
                status = error.message?.takeIf { it.isNotBlank() } ?: "Sample data"
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        runCatching { dataSource.instruments() }
            .onSuccess { remoteInstruments ->
                instruments = remoteInstruments
                selectedInstrumentId = remoteInstruments.firstOrNull { it.id == "BTCUSDT" }?.id
                    ?: remoteInstruments.firstOrNull()?.id
                    ?: selectedInstrumentId
            }
            .onFailure { status = it.message?.takeIf(String::isNotBlank) ?: "Sample data" }
        isLoading = false
        loadCandles(selectedInstrumentId, selectedBar)
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color.Black,
            surface = Color.Black,
            onSurface = Color(0xFFEDEEF2),
            onSurfaceVariant = Color(0xFF7F818A),
        ),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            val subIndicators = selectedSubIndicators.toSubIndicators()
            val customIndicators = if (VwapIndicatorId in selectedSubIndicators) {
                listOf(SampleVwapIndicator)
            } else {
                emptyList()
            }
            val historyMarkers = remember(candles) { candles.toSampleHistoryMarkers() }
            val volumePanelHeight = if ("VOL" in selectedSubIndicators) 104.dp else 0.dp
            val indicatorPanelsHeight = (104 * (subIndicators.size + customIndicators.size)).dp
            val chartHeight = maxWidth + 16.dp + volumePanelHeight + indicatorPanelsHeight

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState()),
            ) {
                MarketHeader(
                    instrument = selectedInstrumentId,
                    instruments = instruments.map { it.id }.ifEmpty { listOf(selectedInstrumentId) },
                    isLoading = isLoading,
                    candles = candles,
                    status = status,
                    onInstrumentSelected = { value ->
                        selectedInstrumentId = value
                        loadCandles(value, selectedBar)
                    },
                )

                PeriodSelector(
                    selected = selectedBar,
                    periods = SampleBars,
                    onSelect = { value ->
                        selectedBar = value
                        loadCandles(selectedInstrumentId, value)
                    },
                )

                Box(Modifier.fillMaxWidth().height(chartHeight)) {
                    KlineChart(
                        dataState = dataState,
                        modifier = Modifier.fillMaxSize(),
                        config = KlineChartConfig(
                            chartStyle = ChartStyle.Candlestick,
                            pricePrecision = 2,
                            showCrosshairInfoPanel = false,
                            showVolume = "VOL" in selectedSubIndicators,
                            volumeHeight = 104.dp,
                            indicatorHeight = 104.dp,
                            mainIndicators = selectedMainIndicators.toMainIndicators(),
                            subIndicators = subIndicators,
                            maPeriods = listOf(7, 20, 99),
                            volumeMaPeriods = listOf(5, 10),
                            timeAxisLabelCount = 3,
                            timeLabelFormatter = ::formatAxisTime,
                        ),
                        colors = TradingChartColors,
                        historyMarkers = historyMarkers,
                        customIndicators = customIndicators,
                        onHistoryMarkerClick = { marker ->
                            status = "Marker ${marker.label} @ ${marker.price.formatPrice()}"
                        },
                    )
                }

                IndicatorTabBar(
                    selectedMain = selectedMainIndicators,
                    selectedSub = selectedSubIndicators,
                    onSelect = { tab ->
                        if (tab in MainIndicatorTabs) {
                            selectedMainIndicators = selectedMainIndicators.toggle(tab)
                        } else {
                            selectedSubIndicators = selectedSubIndicators.toggle(tab)
                        }
                    },
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(192.dp)
                        .background(Color.Black),
                )
            }
        }
    }
}

private val TradingChartColors = KlineChartColors(
    background = Color.Black,
    grid = Color(0xFF16181D),
    textPrimary = Color(0xFFEDEEF2),
    textSecondary = Color(0xFF8B8D96),
    rising = Color(0xFF00B386),
    falling = Color(0xFFEF3F7B),
    crosshair = Color(0xFFEDEEF2),
    labelBackground = Color(0xFF17191F),
    labelText = Color(0xFFEDEEF2),
    indicator1 = Color(0xFFFF4FC3),
    indicator2 = Color(0xFFFF8B1A),
    indicator3 = Color(0xFFD000FF),
    indicator4 = Color(0xFF3D7BFF),
)

private val SampleVwapIndicator = KlineCustomIndicator(
    id = VwapIndicatorId,
    label = VwapIndicatorId,
    preferredColor = Color(0xFF3D7BFF),
    calculator = { candles ->
        var cumulativePriceVolume = 0.0
        var cumulativeVolume = 0.0
        val values = candles.map { candle ->
            val typicalPrice = (candle.high + candle.low + candle.close) / 3.0
            cumulativePriceVolume += typicalPrice * candle.volume
            cumulativeVolume += candle.volume
            if (cumulativeVolume == 0.0) null else cumulativePriceVolume / cumulativeVolume
        }
        KlineIndicatorValues(series = mapOf(VwapIndicatorId to values))
    },
    renderer = { context ->
        val values = context.values.series[VwapIndicatorId].orEmpty()
        val path = Path()
        var started = false
        context.visibleRange.forEach { index ->
            val value = values.getOrNull(index) ?: return@forEach
            val x = index * (context.candleWidth + context.candleSpacing) +
                context.xOffset +
                context.candleWidth / 2f
            val y = value.toPanelY(context.minValue, context.maxValue, size.height)
            if (started) {
                path.lineTo(x, y)
            } else {
                path.moveTo(x, y)
                started = true
            }
        }
        drawPath(
            path = path,
            color = Color(0xFF3D7BFF),
            style = Stroke(width = 1.4.dp.toPx()),
        )
    },
)

private fun List<OhlcvCandle>.toSampleHistoryMarkers(): List<KlineHistoryMarker> {
    if (isEmpty()) return emptyList()
    return listOfNotNull(
        getOrNull((size * 0.72f).toInt())?.let { candle ->
            KlineHistoryMarker(
                timestamp = candle.timestamp,
                price = candle.low,
                label = "B",
                placement = KlineMarkerPlacement.Below,
            )
        },
        getOrNull((size * 0.86f).toInt())?.let { candle ->
            KlineHistoryMarker(
                timestamp = candle.timestamp,
                price = candle.high,
                label = "S",
                placement = KlineMarkerPlacement.Above,
            )
        },
    )
}

private fun Double.toPanelY(minValue: Double, maxValue: Double, height: Float): Float {
    if (maxValue <= minValue) return height / 2f
    return (height - ((this - minValue) / (maxValue - minValue) * height)).toFloat()
}

@Composable
private fun MarketHeader(
    instrument: String,
    instruments: List<String>,
    isLoading: Boolean,
    candles: List<OhlcvCandle>,
    status: String,
    onInstrumentSelected: (String) -> Unit,
) {
    val last = candles.lastOrNull()
    val previous = candles.getOrNull(candles.lastIndex - 1)
    val changePercent = if (last != null && previous != null && previous.close != 0.0) {
        (last.close - previous.close) / previous.close * 100.0
    } else {
        0.0
    }
    val high = candles.maxOfOrNull { it.high } ?: 0.0
    val low = candles.minOfOrNull { it.low } ?: 0.0
    val volume = candles.sumOf { it.volume }
    val turnover = candles.sumOf { it.volume * it.close }
    val changeColor = if (changePercent >= 0.0) Color(0xFF00B386) else Color(0xFFEF3F7B)

    Surface(
        modifier = Modifier.fillMaxWidth().height(176.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("‹", color = MaterialTheme.colorScheme.onSurface, fontSize = 28.sp, lineHeight = 28.sp)
                AppBarDropdown(
                    value = instrument,
                    options = instruments,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                    labelStyle = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 18.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    onSelect = onInstrumentSelected,
                )

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Last Price", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Text(
                        text = last?.close?.formatPrice() ?: "--",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 30.sp,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("≈$${last?.close?.formatPrice() ?: "--"}", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                        Text(
                            text = "${if (changePercent >= 0.0) "+" else ""}${changePercent.formatPercent()}%",
                            color = changeColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    StatRow("24H High", high.formatPrice())
                    StatRow("24H Low", low.formatPrice())
                    StatRow("24H Vol(${instrument.baseSymbol()})", volume.formatCompact())
                    StatRow("24H Turnover(USDT)", turnover.formatCompact())
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, lineHeight = 12.sp, maxLines = 1)
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun PeriodSelector(
    selected: String,
    periods: List<String>,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(Color.Black)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        periods.forEach { period ->
            val selectedPeriod = period == selected
            Text(
                text = period,
                modifier = Modifier
                    .background(
                        color = if (selectedPeriod) Color(0xFF22242B) else Color.Transparent,
                        shape = RoundedCornerShape(6.dp),
                    )
                    .clickable { onSelect(period) }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                color = if (selectedPeriod) Color(0xFFEDEEF2) else Color(0xFF8B8D96),
                fontSize = 14.sp,
                fontWeight = if (selectedPeriod) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun IndicatorTabBar(
    selectedMain: Set<String>,
    selectedSub: Set<String>,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color.Black)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IndicatorTabs.forEach { tab ->
            val isSelected = tab in selectedMain || tab in selectedSub
            Text(
                text = tab,
                modifier = Modifier
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                color = if (isSelected) Color(0xFFEDEEF2) else Color(0xFF8B8D96),
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

private fun Set<String>.toMainIndicators(): Set<MainIndicator> {
    return mapNotNullTo(mutableSetOf()) { indicator ->
        when (indicator) {
            "MA" -> MainIndicator.MA
            "EMA" -> MainIndicator.EMA
            "BOLL" -> MainIndicator.BOLL
            "SAR" -> MainIndicator.SAR
            else -> null
        }
    }
}

private fun Set<String>.toSubIndicators(): List<SubIndicator> {
    return listOfNotNull(
        SubIndicator.MACD.takeIf { "MACD" in this },
        SubIndicator.RSI.takeIf { "RSI" in this },
        SubIndicator.KDJ.takeIf { "KDJ" in this },
    )
}

private fun Set<String>.toggle(value: String): Set<String> {
    return if (value in this) {
        this - value
    } else {
        this + value
    }
}

@Composable
private fun AppBarDropdown(
    value: String,
    options: List<String>,
    modifier: Modifier = Modifier,
    labelStyle: androidx.compose.ui.text.TextStyle,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Text(
            text = value,
            modifier = Modifier
                .clickable { expanded = true }
                .padding(vertical = 10.dp),
            color = MaterialTheme.colorScheme.onSurface,
            style = labelStyle,
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(x = 0.dp, y = 4.dp),
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

private fun Double.formatPrice(): String {
    return formatNumber(decimals = 1)
}

private fun Double.formatPercent(): String {
    return formatNumber(decimals = 2)
}

private fun Double.formatCompact(): String {
    val abs = kotlin.math.abs(this)
    return when {
        abs >= 1_000_000_000.0 -> "${(this / 1_000_000_000.0).formatNumber(2)}B"
        abs >= 1_000_000.0 -> "${(this / 1_000_000.0).formatNumber(2)}M"
        abs >= 1_000.0 -> "${(this / 1_000.0).formatNumber(2)}K"
        else -> formatNumber(2)
    }
}

private fun Double.formatNumber(decimals: Int): String {
    val factor = when (decimals) {
        0 -> 1.0
        1 -> 10.0
        2 -> 100.0
        else -> 1000.0
    }
    val rounded = kotlin.math.round(this * factor) / factor
    val text = rounded.toString()
    val dotIndex = text.indexOf('.')
    val integerPart = if (dotIndex >= 0) text.substring(0, dotIndex) else text
    val fractionPart = if (dotIndex >= 0) text.substring(dotIndex + 1) else ""
    val grouped = integerPart.reversed().chunked(3).joinToString(",").reversed()
    if (decimals == 0) return grouped
    return grouped + "." + fractionPart.padEnd(decimals, '0').take(decimals)
}

private fun String.baseSymbol(): String {
    return removeSuffix("USDT").ifBlank { this }
}

@Suppress("DEPRECATION")
private fun formatAxisTime(timestamp: Long): String {
    val dateTime = Instant.fromEpochMilliseconds(timestamp)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dateTime.monthNumber.twoDigits()}-${dateTime.dayOfMonth.twoDigits()} " +
        "${dateTime.hour.twoDigits()}:${dateTime.minute.twoDigits()}"
}

private fun Int.twoDigits(): String {
    return if (this < 10) "0$this" else toString()
}

private fun sampleCandles(): List<OhlcvCandle> {
    var price = 100.0
    return List(160) { index ->
        val wave = ((index % 18) - 9) * 0.18
        val open = price
        val close = (open + wave + if (index % 5 == 0) 0.8 else -0.25).coerceAtLeast(1.0)
        val high = maxOf(open, close) + 1.2 + (index % 4) * 0.15
        val low = minOf(open, close) - 1.0 - (index % 3) * 0.12
        price = close
        OhlcvCandle(
            timestamp = 1_700_000_000_000L + index * 60_000L,
            endTimestamp = 1_700_000_000_000L + (index + 1) * 60_000L - 1L,
            open = open,
            high = high,
            low = low,
            close = close,
            volume = 1_000.0 + index * 12.0,
        )
    }
}
