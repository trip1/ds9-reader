package com.example.ds9reader

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.FadeTransition
import com.example.ds9reader.screens.home.HomeScreen

@Composable
fun App() {
    MaterialTheme {
        Surface {
            Navigator(HomeScreen) { navigator ->
                FadeTransition(navigator)
            }
        }
    }
}
