package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = NavyPrimaryContainer,
    onPrimary = SurfacePure,
    primaryContainer = NavyPrimary,
    onPrimaryContainer = IceBluePrimaryFixed,
    inversePrimary = IceBlueFixedDim,
    secondary = SecondarySlate,
    onSecondary = SurfacePure,
    secondaryContainer = IceBlueSecondary,
    onSecondaryContainer = NavyPrimary,
    tertiary = NavyAccent,
    onTertiary = SurfacePure,
    tertiaryContainer = IceBluePrimaryFixed,
    onTertiaryContainer = NavyPrimary,
    background = BackgroundBright,
    onBackground = OnSurface,
    surface = SurfacePure,
    onSurface = OnSurface,
    surfaceVariant = SurfaceContainerLow,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceContainerLowest = SurfacePure,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    outline = OutlineNeutral,
    outlineVariant = BorderSubtle,
    error = DangerRed,
    onError = SurfacePure,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkOnSurfaceVariant,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurfaceContainer,
    onSecondaryContainer = DarkOnSurface,
    tertiary = IceBlueFixedDim,
    onTertiary = NavyPrimary,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceContainerLow,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurfaceContainerLow,
    surfaceContainerHigh = DarkSurfaceContainer,
    surfaceContainerHighest = DarkSurfaceContainerHigh,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    error = ColorTokens.DarkError,
    onError = ColorTokens.DarkOnError
)

private object ColorTokens {
    val DarkError = androidx.compose.ui.graphics.Color(0xFFFFB4AB)
    val DarkOnError = androidx.compose.ui.graphics.Color(0xFF690005)
}

val CalmAlarmShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CalmAlarmTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CalmAlarmTypography,
        shapes = CalmAlarmShapes,
        content = content
    )
}
