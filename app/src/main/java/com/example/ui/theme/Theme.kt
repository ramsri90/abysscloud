package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = NeonRedPrimary,
    onPrimary = AmoledTextPrimary,
    primaryContainer = NeonRedContainer,
    onPrimaryContainer = NeonRedOnContainer,
    secondary = NeonRedSecondary,
    onSecondary = AmoledBackground,
    secondaryContainer = Color(0xFF3B0810),
    onSecondaryContainer = Color(0xFFFFD9DF),
    tertiary = NeonRedAccent,
    onTertiary = Color.White,
    background = AmoledBackground,
    onBackground = AmoledTextPrimary,
    surface = AmoledCardDark,
    onSurface = AmoledTextPrimary,
    surfaceVariant = AmoledSurfaceVariantDark,
    onSurfaceVariant = AmoledTextSecondary,
    outline = AmoledBorderDark,
    error = HumanErrorDark
)

private val LightColorScheme = lightColorScheme(
    primary = TerracottaPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFECE5),
    onPrimaryContainer = Color(0xFF5A1C08),
    secondary = TerracottaSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF6E4DE),
    onSecondaryContainer = Color(0xFF381E15),
    tertiary = SagePrimary,
    onTertiary = Color.White,
    background = WarmLinenLight,
    onBackground = WarmTextPrimaryLight,
    surface = WarmCardLight,
    onSurface = WarmTextPrimaryLight,
    surfaceVariant = WarmSurfaceVariantLight,
    onSurfaceVariant = WarmTextSecondaryLight,
    outline = WarmBorderLight,
    error = HumanError
)


@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
