package com.example.ds9reader

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.example.ds9reader.di.appModule
import com.example.ds9reader.di.platformModule
import kotlinx.browser.document
import org.koin.core.context.startKoin

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    startKoin {
        modules(appModule, platformModule())
    }
    ComposeViewport(document.body!!) {
        App()
    }
}
