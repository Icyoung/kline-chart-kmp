# Changelog

## Unreleased

- Added unified `KlineIndicator`/`KlineIndicatorSeries` abstraction for built-in and custom indicators.
- Moved chart indicator computation onto a background path with exact lookback caching for formulas that support it.
- Updated volume and sub-indicator panels to render from computed indicator series instead of recalculating indicators in each panel.
- Removed the old `KlineIndicatorCache` data-state cache path.
- Added WR and OBV built-in indicators.
- Added `LastPriceMode`, `CrosshairDismiss`, entrance animation, and panel animation configuration.

## 0.1.2

- Changed Maven coordinates back to `io.github.icyoung:kline-chart-kmp`.

## 0.1.1

- Changed Maven coordinates to `dev.icyou:kline-chart-kmp`.

## 0.1.0

- Added Compose Multiplatform K-line chart component.
- Added Android, iOS, Desktop, and Wasm targets.
- Added candlestick and line chart modes.
- Added price axis, time axis, latest price line, latest price label, high/low labels, crosshair, and OHLCV info panel.
- Added volume, MA, EMA, BOLL, SAR, MACD, RSI, KDJ, and volume MA support.
- Added timestamp-based data updates and indicator cache via `KlineChartDataState`.
- Added custom candle renderer, custom indicator renderer, custom panel slot, overlay slot, crosshair label formatter, and crosshair info slot.
- Added generic history markers and price-level overlay lines.
- Added Kotlin Multiplatform core calculations and indicator tests.
