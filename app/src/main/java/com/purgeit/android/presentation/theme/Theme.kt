package com.purgeit.android.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    primaryContainer = BrandContainer,
    onPrimaryContainer = BrandLight,
    secondary = BrandLight,
    onSecondary = BgDeep,
    secondaryContainer = BgCard,
    onSecondaryContainer = TextPrimary,
    tertiary = Success,
    onTertiary = Color.White,
    tertiaryContainer = SuccessContainer,
    onTertiaryContainer = Success,
    background = BgDeep,
    onBackground = TextPrimary,
    surface = BgSurface,
    onSurface = TextPrimary,
    surfaceVariant = BgCard,
    onSurfaceVariant = TextSecondary,
    outline = BgCardBorder,
    error = Danger,
    onError = Color.White,
    errorContainer = DangerContainer,
    onErrorContainer = Danger,
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    onPrimary = Neutral99,
    primaryContainer = Purple90,
    onPrimaryContainer = Purple10,
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    error = Red40,
    onError = Neutral99,
)

@Composable
fun PurgeItTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Always use custom dark scheme for the premium look; no dynamic color
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
