package io.github.icyoung

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.icyoung.model.OhlcvCandle

@Composable
fun rememberKlineChartDataState(initialCandles: List<OhlcvCandle> = emptyList()): KlineChartDataState {
    return remember { KlineChartDataState(initialCandles) }
}

@Stable
class KlineChartDataState(initialCandles: List<OhlcvCandle> = emptyList()) {
    var candles by mutableStateOf(initialCandles)
        private set

    var revision by mutableIntStateOf(0)
        private set

    fun replaceAll(newCandles: List<OhlcvCandle>) {
        candles = newCandles
        revision++
    }

    fun prepend(history: List<OhlcvCandle>): Int {
        if (history.isEmpty()) return 0
        if (candles.isEmpty()) {
            candles = history.distinctBy { it.timestamp }.sortedBy { it.timestamp }
            if (candles.isNotEmpty()) revision++
            return candles.size
        }

        val firstTimestamp = candles.first().timestamp
        val existingTimestamps = candles.asSequence().map { it.timestamp }.toSet()
        val prependCandles = history
            .asSequence()
            .filter { it.timestamp < firstTimestamp }
            .filter { it.timestamp !in existingTimestamps }
            .distinctBy { it.timestamp }
            .sortedBy { it.timestamp }
            .toList()
        if (prependCandles.isEmpty()) return 0

        candles = prependCandles + candles
        revision++
        return prependCandles.size
    }

    fun update(candle: OhlcvCandle): KlineDataUpdateKind {
        val result = updateKind(candle)
        candles = when (result) {
            KlineDataUpdateKind.Appended -> candles + candle
            KlineDataUpdateKind.UpdatedLast -> candles.toMutableList().also { it[it.lastIndex] = candle }
            KlineDataUpdateKind.ReplacedExisting -> candles.toMutableList().also { list ->
                val index = list.indexOfFirst { it.timestamp == candle.timestamp }
                if (index >= 0) list[index] = candle
            }
            KlineDataUpdateKind.IgnoredOutOfOrder -> candles
        }
        if (result != KlineDataUpdateKind.IgnoredOutOfOrder) {
            revision++
        }
        return result
    }

    private fun updateKind(candle: OhlcvCandle): KlineDataUpdateKind {
        if (candles.isEmpty()) return KlineDataUpdateKind.Appended

        val last = candles.last()
        return when {
            candle.timestamp == last.timestamp -> KlineDataUpdateKind.UpdatedLast
            candle.timestamp > last.timestamp -> KlineDataUpdateKind.Appended
            candles.any { it.timestamp == candle.timestamp } -> KlineDataUpdateKind.ReplacedExisting
            else -> KlineDataUpdateKind.IgnoredOutOfOrder
        }
    }
}

enum class KlineDataUpdateKind {
    UpdatedLast,
    Appended,
    ReplacedExisting,
    IgnoredOutOfOrder,
}
