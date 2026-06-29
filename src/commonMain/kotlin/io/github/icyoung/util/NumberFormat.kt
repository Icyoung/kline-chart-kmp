package io.github.icyoung.util

import kotlin.math.pow
import kotlin.math.round

fun Double.formatChartValue(scale: Int = 2): String {
    val factor = 10.0.pow(scale)
    val rounded = round(this * factor) / factor
    val text = rounded.toString()
    val dotIndex = text.indexOf('.')

    if (scale <= 0) return text.substringBefore('.')
    if (dotIndex < 0) return text + "." + "0".repeat(scale)

    val decimals = text.length - dotIndex - 1
    return if (decimals >= scale) {
        text.substring(0, dotIndex + scale + 1)
    } else {
        text + "0".repeat(scale - decimals)
    }
}
