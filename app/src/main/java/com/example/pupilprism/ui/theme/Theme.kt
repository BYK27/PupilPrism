package com.example.pupilprism.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.example.pupilprism.data.model.UserStats

@Composable
fun SpeedReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    userStats: UserStats?,
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

    // Explicitly override container colors so FABs and secondary buttons match your theme
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = primaryColor,
            primaryContainer = primaryColor, // Fixes FABs
            onPrimaryContainer = Color.White,
            secondary = primaryColor,
            secondaryContainer = primaryColor,
            background = tintedBackground,
            surface = tintedBackground,
            surfaceVariant = tintedBackground
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            primaryContainer = primaryColor, // Fixes FABs
            onPrimaryContainer = Color.White,
            secondary = primaryColor,
            secondaryContainer = primaryColor,
            background = tintedBackground,
            surface = tintedBackground,
            surfaceVariant = tintedBackground
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}