package org.dolmis.diyarbakir

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme

private val DolmisLightColors = lightColorScheme(
    primary=Color(0xFF28634F), onPrimary=Color.White,
    primaryContainer=Color(0xFFC9EBD7), onPrimaryContainer=Color(0xFF103D2D),
    secondary=Color(0xFF526454), onSecondary=Color.White,
    secondaryContainer=Color(0xFFD5E8D4), onSecondaryContainer=Color(0xFF243C2A),
    tertiary=Color(0xFF8D512E), onTertiary=Color.White,
    tertiaryContainer=Color(0xFFFFDBBF), onTertiaryContainer=Color(0xFF46240D),
    background=Color(0xFFF7F9F2), onBackground=Color(0xFF18251D),
    surface=Color(0xFFF7F9F2), onSurface=Color(0xFF18251D),
    surfaceVariant=Color(0xFFDEE5D9), onSurfaceVariant=Color(0xFF465448),
    surfaceContainerLowest=Color.White, surfaceContainerLow=Color(0xFFF0F4EA),
    surfaceContainer=Color(0xFFEAEFE4), surfaceContainerHigh=Color(0xFFE4EADC),
    surfaceContainerHighest=Color(0xFFDEE4D6), outline=Color(0xFF758273), outlineVariant=Color(0xFFC5CFC0)
)
private val DolmisDarkColors = darkColorScheme(
    primary=Color(0xFF9FD5B7), onPrimary=Color(0xFF003827),
    primaryContainer=Color(0xFF0D513B), onPrimaryContainer=Color(0xFFBCEFD0),
    secondary=Color(0xFFB6CCB7), onSecondary=Color(0xFF213527),
    secondaryContainer=Color(0xFF394F3E), onSecondaryContainer=Color(0xFFD2E8D2),
    tertiary=Color(0xFFFFB68F), onTertiary=Color(0xFF522000),
    tertiaryContainer=Color(0xFF713A1B), onTertiaryContainer=Color(0xFFFFDBCA),
    background=Color(0xFF101510), onBackground=Color(0xFFE0E9DF),
    surface=Color(0xFF101510), onSurface=Color(0xFFE0E9DF),
    surfaceVariant=Color(0xFF414A41), onSurfaceVariant=Color(0xFFC1CCC0),
    surfaceContainerLowest=Color(0xFF0B100C), surfaceContainerLow=Color(0xFF181E18),
    surfaceContainer=Color(0xFF1D251E), surfaceContainerHigh=Color(0xFF282F29),
    surfaceContainerHighest=Color(0xFF333B34), outline=Color(0xFF8B968B), outlineVariant=Color(0xFF414A41)
)
private fun dolmisTypography(scale: Float): Typography {
    fun s(value: Float)= (value*scale).sp
    return Typography(
        headlineLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=s(32f),lineHeight=s(38f),fontWeight=FontWeight.Medium,letterSpacing=s(-0.8f)),
        headlineMedium=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=s(28f),lineHeight=s(34f),fontWeight=FontWeight.Medium),
        titleLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=s(22f),lineHeight=s(28f),fontWeight=FontWeight.Medium),
        titleMedium=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=s(16f),lineHeight=s(23f),fontWeight=FontWeight.Medium),
        bodyLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=s(16f),lineHeight=s(24f)),
        bodyMedium=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=s(14f),lineHeight=s(20f)),
        bodySmall=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=s(12f),lineHeight=s(17f)),
        labelLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=s(14f),lineHeight=s(20f),fontWeight=FontWeight.Medium),
        labelMedium=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=s(12f),lineHeight=s(16f),fontWeight=FontWeight.Medium)
    )
}
@Composable fun DolmisTheme(mode: String = "system", fontScale: Float = 1f, content: @Composable () -> Unit) {
    val dark = when(mode) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
    MaterialTheme(colorScheme=if(dark) DolmisDarkColors else DolmisLightColors, typography=dolmisTypography(fontScale.coerceIn(0.9f,1.35f)), content=content)
}
