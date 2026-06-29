package io.github.icyoung.sample.data

import io.github.icyoung.model.OhlcvCandle

interface MarketDataSource {
    suspend fun instruments(): List<MarketInstrument>

    suspend fun candles(
        instrumentId: String,
        bar: String,
        limit: Int = 180,
    ): List<OhlcvCandle>
}

data class MarketInstrument(
    val id: String,
    val baseCurrency: String,
    val quoteCurrency: String,
) {
    val displayName: String = "$baseCurrency/$quoteCurrency"
}
