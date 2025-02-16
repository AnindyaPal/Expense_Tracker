package com.example.expensetracker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView


private val DarkColorScheme = darkColorScheme(
    primary = NeonYellow,
    secondary = AccentBlue,
    tertiary = SoftWhite,
    background = DeepBlue,
    surface = DarkBlue,
    onPrimary = DeepBlue,
    onSecondary = SoftWhite,
    onTertiary = DeepBlue,
    onBackground = SoftWhite,
    onSurface = SoftWhite
)

@Composable
fun ExpenseTrackerTheme(
    darkTheme: Boolean = true, // Always use dark theme
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DeepBlue.toArgb()
            window.navigationBarColor = DeepBlue.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

fun Color.getAdaptiveTextColor(): Color {
    return when (this) {
        NeonYellow -> DeepBlue  // When background is neon yellow, use deep blue
        else -> SoftWhite  // Default to soft white for other cases
    }
}

// Extension function for gradient background
fun Modifier.gradientBackground() = this.background(
    brush = Brush.verticalGradient(
        colors = listOf(
            DeepBlue,
            DarkBlue
        )
    )
)

