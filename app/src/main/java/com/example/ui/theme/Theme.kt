package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val PrismDarkColorScheme = darkColorScheme(
    primary = PrismCyan,
    onPrimary = Color(0xFF041A24),
    primaryContainer = Color(0xFF11394F),
    onPrimaryContainer = Color(0xFFBDF4FF),
    secondary = PrismGold,
    onSecondary = Color(0xFF281C00),
    secondaryContainer = Color(0xFF3E2E05),
    onSecondaryContainer = Color(0xFFFFE59E),
    tertiary = PrismCoral,
    onTertiary = Color(0xFF2E0414),
    tertiaryContainer = Color(0xFF4F132A),
    onTertiaryContainer = Color(0xFFFFD3E2),
    background = MidnightBg,
    onBackground = TextPrimary,
    surface = DeepSurface,
    onSurface = TextPrimary,
    surfaceVariant = ElevatedSurface,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = MidnightBg,
    surfaceContainerLow = DeepSurface,
    surfaceContainer = ElevatedSurface,
    surfaceContainerHigh = CardSurface,
    surfaceContainerHighest = Color(0xFF2F2758),
    outline = SurfaceBorder,
    outlineVariant = Color(0xFF2A2350),
    error = PrismCrimson,
    onError = Color(0xFF36000D)
)

val PrismShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun PrismBlockTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PrismDarkColorScheme,
        typography = PrismTypography,
        shapes = PrismShapes,
        content = content
    )
}
