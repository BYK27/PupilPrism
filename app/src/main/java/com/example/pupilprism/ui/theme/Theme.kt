package com.example.pupilprism.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import com.example.pupilprism.data.model.UserStats

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun SpeedReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    userStats: UserStats?, // Inject UserStats here
    content: @Composable () -> Unit
) {
    // Determine the primary color (fallback to default purple if null)
    val primaryColor = userStats?.themeColor?.let { Color(it) } ?: Purple40

    // Create a 10% opacity version of the primary color for the background
    val tintedBackground = if (userStats?.isBackgroundEnabled == true) {
        primaryColor.copy(alpha = 0.1f)
    } else {
        if (darkTheme) Color(0xFF1C1B1F) else Color(0xFFFFFBFE)
    }

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = primaryColor,
            background = tintedBackground,
            surface = tintedBackground
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            background = tintedBackground,
            surface = tintedBackground
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}