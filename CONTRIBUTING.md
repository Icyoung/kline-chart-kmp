# Contributing

Thanks for working on kline-chart-kmp.

## Development

Run the core verification tasks before opening a pull request:

```bash
./gradlew compileCommonMainKotlinMetadata
./gradlew desktopTest
```

Keep host-app concerns outside the library. Trading orders, positions, navigation,
storage, websocket clients, and app themes should be integrated through slots,
formatters, renderers, markers, or custom panels.

## API Changes

Prefer additive API changes until the first stable release. When changing public
types, update `README.md` and add or adjust tests for pure calculations.

## Design Goals

- Kotlin Multiplatform first.
- Compose Multiplatform UI with pure, testable core calculations.
- Configurable defaults without forcing an app-specific design system.
- Extensible renderers, overlays, panels, and formatters.
