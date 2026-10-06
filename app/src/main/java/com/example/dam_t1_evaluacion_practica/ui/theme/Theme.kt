package com.example.dam_t1_evaluacion_practica.ui.theme

import android.app.Activity
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
    primary = SafetyAmber,
    onPrimary = IndustrialNavy,
    primaryContainer = IndustrialPrimaryContainer,
    onPrimaryContainer = Color(0xFFFAF8FF),
    secondary = SafetyAmberDark,
    background = IndustrialPrimaryContainer,
    surface = IndustrialPrimaryContainer,
    error = CriticalRed
)

private val LightColorScheme = lightColorScheme(
    primary = IndustrialNavy,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = IndustrialPrimaryContainer,
    onPrimaryContainer = SurfaceDim,
    secondary = SafetyAmber,
    onSecondary = IndustrialNavy,
    background = SurfaceBright,
    surface = Color(0xFFFFFFFF),
    error = CriticalRed
)

@Composable
fun DAM_T1_Evaluacion_PracticaTheme(
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
        typography = Typography,
        content = content
    )
}