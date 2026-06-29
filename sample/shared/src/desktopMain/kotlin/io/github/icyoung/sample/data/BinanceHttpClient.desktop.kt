package io.github.icyoung.sample.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

actual fun createBinanceHttpClient(): HttpClient = createConfiguredBinanceHttpClient(CIO)
