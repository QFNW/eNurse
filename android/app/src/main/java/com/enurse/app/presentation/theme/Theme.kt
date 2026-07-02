package com.enurse.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 易护品牌色
val Blue500 = Color(0xFF2196F3)
val Blue700 = Color(0xFF1976D2)
val Blue50 = Color(0xFFE3F2FD)
val AlertRed = Color(0xFFD32F2F)
val AlertRedBg = Color(0xFFFFEBEE)
val Green500 = Color(0xFF4CAF50)
val Orange500 = Color(0xFFFF9800)

private val LightColors = lightColorScheme(
    primary = Blue700,
    onPrimary = Color.White,
    primaryContainer = Blue50,
    secondary = Green500,
    error = AlertRed,
    errorContainer = AlertRedBg,
    surface = Color.White,
    background = Color(0xFFF5F5F5)
)

@Composable
fun eNurseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
