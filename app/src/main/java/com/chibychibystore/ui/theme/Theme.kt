package com.chibychibystore.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ChibyPinkLight,
    onPrimary = ChibyPinkDark,
    primaryContainer = ChibyPinkDark,
    onPrimaryContainer = ChibyPinkLight,
    secondary = ChibyYellowSecondary,
    onSecondary = ChibyYellowDark,
    secondaryContainer = ChibyYellowDark,
    onSecondaryContainer = ChibyYellowLight,
    tertiary = ChibyTeal,
    onTertiary = White,
    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    surfaceVariant = Neutral20,
    onSurfaceVariant = Neutral90,
    error = Error,
    onError = White
)

private val LightColorScheme = lightColorScheme(
    primary = ChibyPinkPrimary,
    onPrimary = White,
    primaryContainer = ChibyPinkLight,
    onPrimaryContainer = OnChibyPinkContainer,
    secondary = ChibyYellowSecondary,
    onSecondary = Black,
    secondaryContainer = ChibyYellowLight,
    onSecondaryContainer = OnChibyYellowContainer,
    tertiary = ChibyTealDark,
    onTertiary = White,
    background = Neutral99,
    onBackground = Neutral10,
    surface = White,
    onSurface = Neutral10,
    surfaceVariant = Neutral95,
    onSurfaceVariant = Neutral20,
    error = Error,
    onError = White
)

@Composable
fun ChibyChibyStoreTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disable dynamic color to enforce Chiby Chiby brand identity
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
