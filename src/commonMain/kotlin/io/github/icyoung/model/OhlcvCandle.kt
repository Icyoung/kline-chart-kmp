package io.github.icyoung.model

/**
 * Platform-neutral OHLCV candle.
 *
 * [timestamp] is the candle start timestamp. [endTimestamp] defaults to the
 * same value for point-in-time data and can be set for interval-aware marker
 * matching.
 */
data class OhlcvCandle(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val endTimestamp: Long = timestamp,
)
