package io.github.icyoung.sample.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.icyoung.sample.SampleApp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Kline Chart Sample",
    ) {
        SampleApp()
    }
}
