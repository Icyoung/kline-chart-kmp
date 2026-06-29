# kline-chart-kmp

[![KMP Maven Central](https://img.shields.io/maven-central/v/dev.icyou/kline-chart-kmp?label=KMP%20Maven%20Central&color=7F52FF)](https://central.sonatype.com/artifact/dev.icyou/kline-chart-kmp)
[![GitHub tag](https://img.shields.io/github/v/tag/Icyoung/kline-chart-kmp?label=tag&color=0B7285)](https://github.com/Icyoung/kline-chart-kmp/tags)

Kotlin Multiplatform OHLCV/K-line chart library built with Compose Multiplatform.

The library is host-app agnostic: it does not include exchange APIs, repositories,
navigation, storage, trading models, or app themes. Those concerns are provided by
the consuming app through configuration, formatters, overlays, markers, renderers,
and custom panels.

## Install

```kotlin
commonMain.dependencies {
    implementation("dev.icyou:kline-chart-kmp:0.1.1")
}
```

The package root is:

```kotlin
import io.github.icyoung.KlineChart
```

## Basic Usage

```kotlin
val dataState = rememberKlineChartDataState(initialCandles)

// Replace all data and rebuild indicator cache.
dataState.replaceAll(candles)

// Timestamp-based realtime update:
// - same timestamp as last candle: update last
// - greater timestamp: append
// - existing historical timestamp: replace existing and recompute affected cache
dataState.update(realtimeCandle)

KlineChart(
    dataState = dataState,
    config = KlineChartConfig(
        chartStyle = ChartStyle.Candlestick,
        pricePrecision = 2,
        mainIndicators = setOf(MainIndicator.MA, MainIndicator.BOLL),
        subIndicators = listOf(SubIndicator.MACD, SubIndicator.RSI),
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
- Volume panel and built-in technical indicators: MA, EMA, BOLL, SAR, MACD, RSI, KDJ.
- Incremental data state and indicator cache via `KlineChartDataState`.
- Generic history markers and price-level overlay lines.
- Custom candle renderer, custom indicator renderer, custom panels, and overlay slot.
- Built-in indicator metadata through `KlineBuiltInIndicatorSpecs`.
- Pure core calculations and indicator tests.

## Main APIs

- `KlineChart`: top-level Compose chart.
- `KlineChartConfig`: layout, axes, indicators, precision, interaction, and overlay options.
- `KlineChartColors`: chart styling.
- `KlineChartState`: viewport and crosshair state for host synchronization.
- `KlineChartDataState`: list replacement, timestamp-based updates, and indicator cache.
- `KlineOverlayLine`: generic price-level line and label.
- `KlineHistoryMarker`: above/below candle annotations.
- `KlinePanelSpec`: custom panel slot.
- `KlineCustomIndicator`: custom indicator calculation and drawing.
- `CandleRenderer`: replaceable candle renderer.

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
