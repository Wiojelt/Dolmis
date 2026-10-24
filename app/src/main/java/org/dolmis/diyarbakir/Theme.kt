package org.dolmis.diyarbakir

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val DolmisColors = lightColorScheme(
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
private val DolmisType=Typography(
    headlineLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=32.sp,lineHeight=38.sp,fontWeight=FontWeight.Medium,letterSpacing=(-0.8).sp),
    headlineMedium=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=28.sp,lineHeight=34.sp,fontWeight=FontWeight.Medium),
    titleLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=22.sp,lineHeight=28.sp,fontWeight=FontWeight.Medium),
    titleMedium=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=16.sp,lineHeight=23.sp,fontWeight=FontWeight.Medium),
    bodyLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=16.sp,lineHeight=24.sp),
    bodyMedium=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=14.sp,lineHeight=20.sp),
    bodySmall=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=12.sp,lineHeight=17.sp),
    labelLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=14.sp,lineHeight=20.sp,fontWeight=FontWeight.Medium),
    labelMedium=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=12.sp,lineHeight=16.sp,fontWeight=FontWeight.Medium)
)
@Composable fun DolmisTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme=DolmisColors,typography=DolmisType,content=content)
}
