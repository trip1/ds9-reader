package com.example.ds9reader

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.example.ds9reader.screens.home.HomeScreen
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import com.example.ds9reader.database.LibraryDatabase
import com.example.ds9reader.di.appModule
import com.example.ds9reader.di.platformModule

@Composable
fun App() {
    KoinApplication(
        application = {
            modules(appModule, platformModule())
        }
    ) {
        Navigator(HomeScreen) { navigator ->
            SlideTransition(navigator)
        }
    }
}
