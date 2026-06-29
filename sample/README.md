# kline-chart-kmp sample

Standalone sample project for Android, iOS, and Desktop.

The sample references the library through a relative composite build:

```kotlin
includeBuild("..")
```

The `shared` module depends on the library coordinates:

```kotlin
api(libs.kline.chart.kmp)
```

Gradle resolves that dependency from the parent directory instead of Maven.

The sample uses Binance public REST APIs for market data:

- `GET /api/v3/exchangeInfo`
- `GET /api/v3/klines?symbol=BTCUSDT&interval=1m&limit=180`

Network code lives only in this sample project. The chart library itself stays
free of exchange-specific data sources.

## Modules

- `shared`: common Compose sample UI and Android/iOS/Desktop targets.
- `androidApp`: Android launcher app.
- `desktopApp`: Desktop launcher app.
- `iosApp`: SwiftUI iOS shell that embeds the shared Compose UI.

## Verify

From the library root:

```bash
./gradlew -p sample :shared:compileCommonMainKotlinMetadata
./gradlew -p sample :androidApp:compileDebugKotlin
./gradlew -p sample :desktopApp:compileKotlin
./gradlew -p sample :shared:linkDebugFrameworkIosSimulatorArm64
```

The iOS shell can be checked with:

```bash
xcodebuild -project sample/iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Debug \
  -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' \
  build
```

## Run

```bash
./gradlew -p sample :desktopApp:run
./gradlew -p sample :androidApp:installDebug
open sample/iosApp/iosApp.xcodeproj
```
