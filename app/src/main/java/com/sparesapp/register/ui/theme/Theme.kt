package com.sparesapp.register.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val DarkColors = darkColorScheme(
    background = Bg,
    surface = Panel,
    surfaceVariant = Panel2,
    primary = Accent,
    onPrimary = AccentInk,
    secondary = Accent2,
    onBackground = Text,
    onSurface = Text,
    outline = Border,
    error = Danger,
)

private val LightColors = lightColorScheme(
    background = BgLight,
    surface = PanelLight,
    surfaceVariant = Panel2Light,
    primary = AccentLight,
    onPrimary = Color(0xFFFFF7EA),
    secondary = Accent2Light,
    onBackground = TextLight,
    onSurface = TextLight,
    outline = BorderLight,
    error = DangerLight,
)

val MonoFontFamily = FontFamily.Monospace

@Composable
fun SparesRegisterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography,
        content = content
    )
}

val MonoStyle = TextStyle(fontFamily = MonoFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp)
