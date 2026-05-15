package com.example.ds9reader

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.ds9reader.di.appModule
import com.example.ds9reader.di.platformModule
import org.koin.core.context.startKoin

fun main() = application {
    startKoin {
        modules(appModule, platformModule())
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "DS-9 Reader",
    ) {
        App()
    }
}
