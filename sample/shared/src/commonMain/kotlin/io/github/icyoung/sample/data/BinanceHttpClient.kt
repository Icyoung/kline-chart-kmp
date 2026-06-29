package io.github.icyoung.sample.data

import io.ktor.client.HttpClient

expect fun createBinanceHttpClient(): HttpClient
