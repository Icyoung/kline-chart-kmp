# kline-chart-kmp

[![KMP Maven Central](https://img.shields.io/maven-central/v/io.github.icyoung/kline-chart-kmp?label=KMP%20Maven%20Central&color=7F52FF)](https://central.sonatype.com/artifact/io.github.icyoung/kline-chart-kmp)
[![GitHub tag](https://img.shields.io/github/v/tag/Icyoung/kline-chart-kmp?label=tag&color=0B7285)](https://github.com/Icyoung/kline-chart-kmp/tags)

Kotlin Multiplatform OHLCV/K-line chart library built with Compose Multiplatform.

The library is host-app agnostic: it does not include exchange APIs, repositories,
navigation, storage, trading models, or app themes. Those concerns are provided by
the consuming app through configuration, formatters, overlays, markers, renderers,
and custom panels.

## Install

```kotlin
commonMain.dependencies {
    implementation("io.github.icyoung:kline-chart-kmp:0.1.3")
}
```

The package root is:

```kotlin
import io.github.icyoung.KlineChart
```

## Basic Usage

```kotlin
val dataState = rememberKlineChartDataState(initialCandles)

// Replace all data.
dataState.replaceAll(candles)

// Timestamp-based realtime update:
// - same timestamp as last candle: update last
// - greater timestamp: append
// - existing historical timestamp: replace existing
dataState.update(realtimeCandle)

KlineChart(
    dataState = dataState,
    config = KlineChartConfig(
        chartStyle = ChartStyle.Candlestick,
        pricePrecision = 2,
        mainIndicators = setOf(MainIndicator.MA, MainIndicator.BOLL),
        subIndicators = listOf(SubIndicator.MACD, SubIndicator.RSI, SubIndicator.WR, SubIndicator.OBV),
        lastPriceMode = LastPriceMode.Latest,
        crosshairDismiss = CrosshairDismiss.Persistent,
        timeLabelFormatter = { timestamp -> timestamp.toString() },
    ),
    colors = KlineChartColors(),
)
```

See [sample](sample) for Android, iOS, and Desktop apps backed by Binance public
REST data. The sample uses a relative composite build and is not part of the
published library.

## Features

- Candlestick and line chart modes.
- Price axis and time axis.
- Latest price dashed line and label.
- Visible-range high/low price labels.
- Crosshair with replaceable info panel.
- Pinch zoom, horizontal drag, inertia, and load-more callback.
- Volume panel and built-in technical indicators: MA, EMA, BOLL, SAR, MACD, RSI, KDJ, WR, OBV.
- Background indicator computation with cached realtime tail updates where the formula supports exact lookback calculation.
- Generic `KlineIndicator` registration for main-chart overlays, existing sub-panel overlays, and new sub-panels.
- Configurable latest-price source through `LastPriceMode`.
- Configurable crosshair release behavior through `CrosshairDismiss`.
- Optional entrance and panel add/remove animations.
- Incremental data state via `KlineChartDataState`.
- Generic history markers and price-level overlay lines.
- Custom candle renderer, custom indicator renderer, custom panels, and overlay slot.
- Built-in indicator metadata through `KlineBuiltInIndicatorSpecs`.
- Pure core calculations and indicator tests.

## Main APIs

- `KlineChart`: top-level Compose chart.
- `KlineChartConfig`: layout, axes, indicators, precision, interaction, and overlay options.
- `KlineChartColors`: chart styling.
- `KlineChartState`: viewport and crosshair state for host synchronization.
- `KlineChartDataState`: list replacement, historical prepend, and timestamp-based realtime updates.
- `KlineOverlayLine`: generic price-level line and label.
- `KlineHistoryMarker`: above/below candle annotations.
- `KlinePanelSpec`: custom panel slot.
- `KlineCustomIndicator`: custom indicator calculation and drawing.
- `KlineIndicator`: reusable indicator formula abstraction for built-in and custom series.
- `CandleRenderer`: replaceable candle renderer.

## Custom Indicators

Use `indicators` for reusable formula-based indicators:

```kotlin
KlineChart(
    dataState = dataState,
    indicators = listOf(
        klineIndicator(id = "MA120", pane = KlineIndicatorPane.Main, lookback = 120) { candles ->
            listOf(KlineIndicatorLine("MA120", TechnicalIndicators.calculateMA(candles, 120) { it.close }))
        },
        klineIndicator(id = "MY_MACD_LINE", pane = KlineIndicatorPane.Sub, overlayId = "MACD") { candles ->
            listOf(KlineIndicatorLine("X", candles.map { it.close }))
        },
        klineIndicator(id = "MY_PANEL", pane = KlineIndicatorPane.Sub, label = "MY") { candles ->
            listOf(KlineIndicatorLine("C", candles.map { it.close }))
        },
        klineIndicator(
            id = "BTC_MA20",
            pane = KlineIndicatorPane.Main,
            sourceCandles = btcCandles,
            showLatestValue = true,
        ) { btc ->
            listOf(KlineIndicatorLine("BTC MA20", TechnicalIndicators.calculateMA(btc, 20) { it.close }))
        },
    ),
)
```

- `pane = KlineIndicatorPane.Main`: draw on the main chart.
- `pane = KlineIndicatorPane.Sub` with `overlayId`: draw on an existing sub-panel such as `VOL`, `MACD`, `RSI`, `KDJ`, `WR`, or `OBV`.
- `pane = KlineIndicatorPane.Sub` without `overlayId`: create a new sub-panel.
- `sourceCandles`: calculate the indicator from another candle stream and align it back to the chart candles by timestamp. It uses an independent hidden axis by default, so different price levels can be overlaid as trend lines. Use `alignment = KlineIndicatorAlignment.Index` for index-based alignment, or `scaleMode = KlineIndicatorScaleMode.SharedPriceAxis` when the values should share the main price axis.
- `showLatestValue = true`: show the indicator's latest visible value with the same dashed line and right-side label behavior as the latest price overlay.

Implementation packages under `io.github.icyoung.internal.*` are not stable API.

## Build

From this directory:

```bash
./gradlew compileCommonMainKotlinMetadata
./gradlew desktopTest
```

Sample checks:

```bash
./gradlew -p sample :shared:compileCommonMainKotlinMetadata
./gradlew -p sample :androidApp:compileDebugKotlin
./gradlew -p sample :desktopApp:compileKotlin
./gradlew -p sample :shared:linkDebugFrameworkIosSimulatorArm64
```

iOS sample:

```bash
xcodebuild -project sample/iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Debug \
  -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' \
  build
```
