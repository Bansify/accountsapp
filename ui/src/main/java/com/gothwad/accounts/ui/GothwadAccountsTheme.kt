package com.gothwad.accounts.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
data class AccountSemanticColors(
    val canvasBackground: Color,
    val cardBorder: Color,
    val verifiedGreen: Color,
    val verifiedContainer: Color,
    val onVerifiedContainer: Color,
    val warningAmber: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val criticalRed: Color,
    val criticalContainer: Color,
    val onCriticalContainer: Color,
    val brandPrimary: Color,
    val brandSecondary: Color,
    val brandTertiary: Color,
    val brandHighlight: Color,
)

private val LightSemanticColors = AccountSemanticColors(
    canvasBackground = Color(0xFFF0F4F9),
    cardBorder = Color(0xFFE0E3E7),
    verifiedGreen = Color(0xFF146C2E),
    verifiedContainer = Color(0xFFC4EED0),
    onVerifiedContainer = Color(0xFF072711),
    warningAmber = Color(0xFFB06000),
    warningContainer = Color(0xFFFEF7E0),
    onWarningContainer = Color(0xFF472500),
    criticalRed = Color(0xFFB3261E),
    criticalContainer = Color(0xFFFCE8E6),
    onCriticalContainer = Color(0xFF410E0B),
    brandPrimary = Color(0xFF0B57D0),
    brandSecondary = Color(0xFF146C2E),
    brandTertiary = Color(0xFFB06000),
    brandHighlight = Color(0xFFB3261E),
)

private val DarkSemanticColors = AccountSemanticColors(
    canvasBackground = Color(0xFF111318),
    cardBorder = Color(0xFF2C3038),
    verifiedGreen = Color(0xFF6DD58C),
    verifiedContainer = Color(0xFF0F3D1E),
    onVerifiedContainer = Color(0xFFC4EED0),
    warningAmber = Color(0xFFFCC934),
    warningContainer = Color(0xFF3F2E00),
    onWarningContainer = Color(0xFFFFE082),
    criticalRed = Color(0xFFF28B82),
    criticalContainer = Color(0xFF531B16),
    onCriticalContainer = Color(0xFFF9DEDC),
    brandPrimary = Color(0xFFA8C7FA),
    brandSecondary = Color(0xFF6DD58C),
    brandTertiary = Color(0xFFFCC934),
    brandHighlight = Color(0xFFF28B82),
)

val LocalAccountColors = staticCompositionLocalOf { LightSemanticColors }

private val AccountsLightColorScheme = lightColorScheme(
    primary = Color(0xFF0B57D0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3E3FD),
    onPrimaryContainer = Color(0xFF041E49),
    secondary = Color(0xFF00639B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC2E7FF),
    onSecondaryContainer = Color(0xFF001D35),
    tertiary = Color(0xFF146C2E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC4EED0),
    onTertiaryContainer = Color(0xFF072711),
    background = Color(0xFFF0F4F9),
    onBackground = Color(0xFF1F1F1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F1F1F),
    surfaceVariant = Color(0xFFE1E5EA),
    onSurfaceVariant = Color(0xFF444746),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8FAFD),
    surfaceContainer = Color(0xFFF0F4F9),
    surfaceContainerHigh = Color(0xFFE9EEF6),
    surfaceContainerHighest = Color(0xFFDDE3EA),
    outline = Color(0xFF747775),
    outlineVariant = Color(0xFFC4C7C5),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

private val AccountsDarkColorScheme = darkColorScheme(
    primary = Color(0xFFA8C7FA),
    onPrimary = Color(0xFF062E6F),
    primaryContainer = Color(0xFF0842A0),
    onPrimaryContainer = Color(0xFFD3E3FD),
    secondary = Color(0xFF7FCFFF),
    onSecondary = Color(0xFF003355),
    secondaryContainer = Color(0xFF004A77),
    onSecondaryContainer = Color(0xFFC2E7FF),
    tertiary = Color(0xFF6DD58C),
    onTertiary = Color(0xFF0A3818),
    tertiaryContainer = Color(0xFF0F5223),
    onTertiaryContainer = Color(0xFFC4EED0),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE3E3E3),
    surface = Color(0xFF1B1E24),
    onSurface = Color(0xFFE3E3E3),
    surfaceVariant = Color(0xFF282C34),
    onSurfaceVariant = Color(0xFFC4C7C5),
    surfaceContainerLowest = Color(0xFF14171C),
    surfaceContainerLow = Color(0xFF1B1E24),
    surfaceContainer = Color(0xFF20242C),
    surfaceContainerHigh = Color(0xFF282D37),
    surfaceContainerHighest = Color(0xFF323844),
    outline = Color(0xFF8E918F),
    outlineVariant = Color(0xFF444746),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
)

val DisplayFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold),
)

val BodyFontFamily = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold),
)

val AccountsTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.3).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.2).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
)

val AccountsShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun GothwadAccountsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) AccountsDarkColorScheme else AccountsLightColorScheme
    val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors
    CompositionLocalProvider(LocalAccountColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AccountsTypography,
            shapes = AccountsShapes,
            content = content,
        )
    }
}
