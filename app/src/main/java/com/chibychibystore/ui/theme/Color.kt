package com.chibychibystore.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Primary Brand Colors (Refined Chiby Pink)
val ChibyPinkPrimary = Color(0xFFE91E63) // Vibrant Pink
val ChibyPinkLight = Color(0xFFFCE4EC) // Soft Pink Background
val ChibyPinkDark = Color(0xFF880E4F) // Deep Pink for Contrast
val ChibyPinkGradientStart = Color(0xFFEC407A)
val ChibyPinkGradientEnd = Color(0xFFC2185B)

// Secondary Brand Colors (Golden Amber)
val ChibyYellowSecondary = Color(0xFFFFC107) // Amber 500
val ChibyYellowLight = Color(0xFFFFF8E1) // Soft Amber
val ChibyYellowDark = Color(0xFFFFA000) // Deep Amber

// Tertiary / Accent Colors
val ChibyTeal = Color(0xFF009688)
val ChibyBlue = Color(0xFF2196F3)
val ChibyPurple = Color(0xFF9C27B0)

// Surface & Background Colors (Modern Neutrals)
val Neutral10 = Color(0xFF1C1B1F) // Dark Background
val Neutral20 = Color(0xFF49454F) // Dark Surface
val Neutral90 = Color(0xFFE7E0EC)
val Neutral95 = Color(0xFFF3EDF7) // Light Surface Variant
val Neutral98 = Color(0xFFFDFCFD) // Almost White
val Neutral99 = Color(0xFFFFFBFE) // Light Background
val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)

// Semantic Colors
val Success = Color(0xFF4CAF50)
val Warning = Color(0xFFFF9800)
val Error = Color(0xFFB3261E)
val Info = Color(0xFF2196F3)

// Gradients
val PrimaryGradient = Brush.horizontalGradient(
    colors = listOf(ChibyPinkGradientStart, ChibyPinkGradientEnd)
)

val SurfaceGradient = Brush.verticalGradient(
    colors = listOf(White, Neutral98)
)
