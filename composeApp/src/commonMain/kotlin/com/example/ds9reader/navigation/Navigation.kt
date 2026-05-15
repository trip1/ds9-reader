package com.example.ds9reader.navigation

import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import androidx.compose.runtime.Composable

@Composable
fun AppNavigator(initialScreen: Screen) {
    Navigator(initialScreen) { navigator ->
        SlideTransition(navigator)
    }
}
