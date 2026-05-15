package com.example.ds9reader

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "DS-9 Reader",
    ) {
        App()
    }
}
