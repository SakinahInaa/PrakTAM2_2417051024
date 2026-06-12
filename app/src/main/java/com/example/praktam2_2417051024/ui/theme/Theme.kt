package com.example.praktam2_2417051024.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun DailyCheckTheme(
    themeColor: String = "Hijau",
    isDarkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeColor) {
        "Biru" -> {
            if (isDarkTheme) {
                darkColorScheme(
                    primary = BlueSecondary,
                    secondary = BlueSecondary,
                    background = BlueBackgroundDark,
                    surface = CardSurfaceDark,
                    onPrimary = Color.Black,
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            } else {
                lightColorScheme(
                    primary = BluePrimary,
                    secondary = BlueSecondary,
                    background = BlueBackground,
                    surface = CardSurfaceLight,
                    onPrimary = OnPrimaryText,
                    onBackground = Color.Black,
                    onSurface = Color.Black
                )
            }
        }
        "Ungu" -> {
            if (isDarkTheme) {
                darkColorScheme(
                    primary = PurpleSecondary,
                    secondary = PurpleSecondary,
                    background = PurpleBackgroundDark,
                    surface = CardSurfaceDark,
                    onPrimary = Color.Black,
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            } else {
                lightColorScheme(
                    primary = PurplePrimary,
                    secondary = PurpleSecondary,
                    background = PurpleBackground,
                    surface = CardSurfaceLight,
                    onPrimary = OnPrimaryText,
                    onBackground = Color.Black,
                    onSurface = Color.Black
                )
            }
        }
        else -> {
            if (isDarkTheme) {
                darkColorScheme(
                    primary = GreenSecondary,
                    secondary = GreenSecondary,
                    background = GreenBackgroundDark,
                    surface = CardSurfaceDark,
                    onPrimary = Color.Black,
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            } else {
                lightColorScheme(
                    primary = GreenPrimary,
                    secondary = GreenSecondary,
                    background = GreenBackground,
                    surface = CardSurfaceLight,
                    onPrimary = OnPrimaryText,
                    onBackground = Color.Black,
                    onSurface = Color.Black
                )
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}