package io.github.icyoung

object KlineBuiltInIndicatorSpecs {
    val MA = MainIndicatorSpec(
        id = "MA",
        label = "MA",
        parameters = listOf(period("period1", "Period 1", 7.0), period("period2", "Period 2", 25.0), period("period3", "Period 3", 99.0)),
    )

    val EMA = MainIndicatorSpec(
        id = "EMA",
        label = "EMA",
        parameters = listOf(period("period1", "Period 1", 7.0), period("period2", "Period 2", 25.0), period("period3", "Period 3", 99.0)),
    )

    val BOLL = MainIndicatorSpec(
        id = "BOLL",
        label = "BOLL",
        parameters = listOf(
            period("period", "Period", 20.0),
            KlineIndicatorParameterSpec("stdDev", "Std Dev", 2.0, 0.5..5.0, step = 0.1),
        ),
    )

    val SAR = MainIndicatorSpec(
        id = "SAR",
        label = "SAR",
        parameters = listOf(
            KlineIndicatorParameterSpec("acceleration", "Acceleration", 0.02, 0.01..0.2, step = 0.01),
            KlineIndicatorParameterSpec("maxAcceleration", "Max Acceleration", 0.2, 0.02..1.0, step = 0.01),
        ),
    )

    val VOL = SubIndicatorSpec(
        id = "VOL",
        label = "VOL",
        parameters = listOf(period("ma1", "MA 1", 5.0), period("ma2", "MA 2", 10.0)),
    )

    val MACD = SubIndicatorSpec(
        id = "MACD",
        label = "MACD",
        parameters = listOf(period("fast", "Fast", 12.0), period("slow", "Slow", 26.0), period("signal", "Signal", 9.0)),
    )

    val RSI = SubIndicatorSpec(
        id = "RSI",
        label = "RSI",
        parameters = listOf(period("period1", "Period 1", 6.0), period("period2", "Period 2", 12.0), period("period3", "Period 3", 24.0)),
    )

    val KDJ = SubIndicatorSpec(
        id = "KDJ",
        label = "KDJ",
        parameters = listOf(period("period", "Period", 9.0), period("kSmooth", "K Smooth", 3.0), period("dSmooth", "D Smooth", 3.0)),
    )

    val mainIndicators = listOf(MA, EMA, BOLL, SAR)
    val subIndicators = listOf(VOL, MACD, RSI, KDJ)

    private fun period(id: String, label: String, defaultValue: Double): KlineIndicatorParameterSpec {
        return KlineIndicatorParameterSpec(id, label, defaultValue, 1.0..250.0, step = 1.0)
    }
}
