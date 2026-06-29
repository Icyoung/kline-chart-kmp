package io.github.icyoung.sample.data

import io.github.icyoung.model.OhlcvCandle
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class BinanceMarketDataSource(
    private val client: HttpClient = createBinanceHttpClient(),
) : MarketDataSource {
    override suspend fun instruments(): List<MarketInstrument> {
        val response = client.get("$BinanceBaseUrl/api/v3/exchangeInfo")
            .body<BinanceExchangeInfoDto>()

        return response.symbols
            .asSequence()
            .filter { it.status == "TRADING" }
            .filter { it.quoteAsset == "USDT" }
            .filter { it.isSpotTradingAllowed }
            .map {
                MarketInstrument(
                    id = it.symbol,
                    baseCurrency = it.baseAsset,
                    quoteCurrency = it.quoteAsset,
                )
            }
            .sortedBy { it.id }
            .toList()
    }

    override suspend fun candles(
        instrumentId: String,
        bar: String,
        limit: Int,
    ): List<OhlcvCandle> {
        val response = client.get("$BinanceBaseUrl/api/v3/klines") {
            parameter("symbol", instrumentId)
            parameter("interval", bar)
            parameter("limit", limit.coerceIn(1, 1000))
        }.body<List<List<JsonElement>>>()

        return response.mapNotNull { row -> row.toCandle() }
    }
}

private const val BinanceBaseUrl = "https://api.binance.com"

@Serializable
private data class BinanceExchangeInfoDto(
    val symbols: List<BinanceSymbolDto>,
)

@Serializable
private data class BinanceSymbolDto(
    val symbol: String,
    val status: String,
    val baseAsset: String,
    val quoteAsset: String,
    val isSpotTradingAllowed: Boolean = false,
)

private fun List<JsonElement>.toCandle(): OhlcvCandle? {
    if (size < 7) return null
    val openTime = longAt(0) ?: return null
    val open = doubleAt(1) ?: return null
    val high = doubleAt(2) ?: return null
    val low = doubleAt(3) ?: return null
    val close = doubleAt(4) ?: return null
    val volume = doubleAt(5) ?: 0.0
    val closeTime = longAt(6) ?: openTime
    return OhlcvCandle(
        timestamp = openTime,
        endTimestamp = closeTime,
        open = open,
        high = high,
        low = low,
        close = close,
        volume = volume,
    )
}

private fun List<JsonElement>.longAt(index: Int): Long? {
    return getOrNull(index)?.jsonPrimitive?.longOrNull
        ?: getOrNull(index)?.jsonPrimitive?.content?.toLongOrNull()
}

private fun List<JsonElement>.doubleAt(index: Int): Double? {
    return getOrNull(index)?.jsonPrimitive?.doubleOrNull
        ?: getOrNull(index)?.jsonPrimitive?.content?.toDoubleOrNull()
}
