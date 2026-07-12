package com.example.ds9reader

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.FadeTransition
import com.example.ds9reader.domain.LibraryRepository
import com.example.ds9reader.domain.ThemeMode
import com.example.ds9reader.screens.main.MainScreen
import com.example.ds9reader.ui.theme.Ds9Theme
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun App() {
    val repository = koinInject<LibraryRepository>()
    val scope = rememberCoroutineScope()
    var themeMode by remember { mutableStateOf(ThemeMode.System) }

    LaunchedEffect(repository) {
        themeMode = repository.getThemeMode()
    }

    Ds9Theme(themeMode = themeMode) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Navigator(
                MainScreen(
                    themeMode = themeMode,
                    onCycleTheme = {
                        val next = when (themeMode) {
                            ThemeMode.System -> ThemeMode.Light
                            ThemeMode.Light -> ThemeMode.Dark
                            ThemeMode.Dark -> ThemeMode.System
                        }
                        themeMode = next
                        scope.launch { repository.setThemeMode(next) }
                    },
                ),
            ) { navigator ->
                FadeTransition(navigator)
            }
        }
    }
}
