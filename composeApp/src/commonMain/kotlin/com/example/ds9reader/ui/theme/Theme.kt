package com.example.ds9reader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.ds9reader.domain.ThemeMode

// Warm paper / ink palette for a reading app
private val LightPrimary = Color(0xFF3F5B4A)
private val LightOnPrimary = Color(0xFFFFFFFF)
private val LightPrimaryContainer = Color(0xFFD5E8DB)
private val LightOnPrimaryContainer = Color(0xFF102117)
private val LightSecondary = Color(0xFF6B4F3A)
private val LightOnSecondary = Color(0xFFFFFFFF)
private val LightSecondaryContainer = Color(0xFFF3DFD0)
private val LightOnSecondaryContainer = Color(0xFF26170C)
private val LightTertiary = Color(0xFF4A5B78)
private val LightOnTertiary = Color(0xFFFFFFFF)
private val LightBackground = Color(0xFFF7F3EA)
private val LightOnBackground = Color(0xFF1C1B17)
private val LightSurface = Color(0xFFFFFBF4)
private val LightOnSurface = Color(0xFF1C1B17)
private val LightSurfaceVariant = Color(0xFFE6E1D6)
private val LightOnSurfaceVariant = Color(0xFF49463E)
private val LightOutline = Color(0xFF7A766C)
private val LightError = Color(0xFFB3261E)

private val DarkPrimary = Color(0xFFB4CFBE)
private val DarkOnPrimary = Color(0xFF1A3326)
private val DarkPrimaryContainer = Color(0xFF2F4438)
private val DarkOnPrimaryContainer = Color(0xFFD5E8DB)
private val DarkSecondary = Color(0xFFD6BDA8)
private val DarkOnSecondary = Color(0xFF3B2718)
private val DarkSecondaryContainer = Color(0xFF533D2C)
private val DarkOnSecondaryContainer = Color(0xFFF3DFD0)
private val DarkTertiary = Color(0xFFB4C4E0)
private val DarkOnTertiary = Color(0xFF1E2C44)
private val DarkBackground = Color(0xFF121410)
private val DarkOnBackground = Color(0xFFE6E2D9)
private val DarkSurface = Color(0xFF1A1C18)
private val DarkOnSurface = Color(0xFFE6E2D9)
private val DarkSurfaceVariant = Color(0xFF49463E)
private val DarkOnSurfaceVariant = Color(0xFFCBC6BB)
private val DarkOutline = Color(0xFF958F85)
private val DarkError = Color(0xFFF2B8B5)

private val LightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    error = LightError,
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    error = DarkError,
)

@Composable
fun Ds9Theme(
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content,
    )
}
