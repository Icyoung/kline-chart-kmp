package io.github.icyoung.sample.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

actual fun createBinanceHttpClient(): HttpClient = createConfiguredBinanceHttpClient(Darwin)
