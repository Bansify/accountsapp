package com.gothwad.accounts.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/**
 * Canonical color tokens for Gothwad Accounts (see `docs/THEME_SYSTEM.md`).
 *
 * Design contract — keep all future UI updates strictly inside these two tonal scales:
 * - Light theme: Pure neutral `#FFFFFF` tonal scale (`#FFFFFF` -> `#F8F8F8` -> `#F3F3F3` -> `#EDEDED` -> `#E5E5E5`)
 *   so main background, top bar, status bar, and 3-button Android navigation bar stay seamless `#FFFFFF`.
 * - Dark theme: Pure neutral `#212121` tonal scale (`#212121` -> `#282828` -> `#2E2E2E` -> `#353535` -> `#3F3F3F`)
 *   so main background, top bar, status bar, and 3-button Android navigation bar stay seamless `#212121`.
 */
private object AccountPaletteTokens {
    // --- Light Theme (#FFFFFF Tonal Scale) ---
    val LightCanvas = Color(0xFFFFFFFF)             // Main UI background, status bar, 3-button nav bar, top app bar
    val LightSurfaceLowest = Color(0xFFFFFFFF)      // Inner hero cards & search pill
    val LightSurfaceLow = Color(0xFFF8F8F8)         // Primary grouped cards (AccountSectionCard, tiles)
    val LightSurfaceContainer = Color(0xFFF3F3F3)   // Floating dialogs / sheets & secondary cards
    val LightSurfaceHigh = Color(0xFFEDEDED)        // Icon circle badges, chip backgrounds, progress tracks
    val LightSurfaceHighest = Color(0xFFE5E5E5)     // High-emphasis neutral containers
    val LightSurfaceVariant = Color(0xFFF2F2F2)
    val LightBorder = Color(0xFFE6E6E6)             // Hairline card border & section dividers
    val LightOutline = Color(0xFF757575)
    val LightOnSurface = Color(0xFF1A1A1A)
    val LightOnSurfaceVariant = Color(0xFF5C5C5C)

    // --- Dark Theme (#212121 Tonal Scale) ---
    val DarkCanvas = Color(0xFF212121)              // Main UI background, status bar, 3-button nav bar, top app bar
    val DarkSurfaceLowest = Color(0xFF1B1B1B)       // Recessed / sunken containers
    val DarkSurfaceLow = Color(0xFF282828)          // Primary grouped cards (AccountSectionCard, tiles)
    val DarkSurfaceContainer = Color(0xFF2E2E2E)    // Floating dialogs / sheets & secondary cards
    val DarkSurfaceHigh = Color(0xFF353535)         // Icon circle badges, chip backgrounds, progress tracks
    val DarkSurfaceHighest = Color(0xFF3F3F3F)      // High-emphasis neutral containers
    val DarkSurfaceVariant = Color(0xFF303030)
    val DarkBorder = Color(0xFF383838)              // Hairline card border & section dividers
    val DarkOutline = Color(0xFF9E9E9E)
    val DarkOnSurface = Color(0xFFF5F5F5)
    val DarkOnSurfaceVariant = Color(0xFFBDBDBD)
}

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
    canvasBackground = AccountPaletteTokens.LightCanvas,
    cardBorder = AccountPaletteTokens.LightBorder,
    verifiedGreen = Color(0xFF146C2E),
    verifiedContainer = Color(0xFFD6F3DE),
    onVerifiedContainer = Color(0xFF072711),
    warningAmber = Color(0xFFB06000),
    warningContainer = Color(0xFFFEF3D6),
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
    canvasBackground = AccountPaletteTokens.DarkCanvas,
    cardBorder = AccountPaletteTokens.DarkBorder,
    verifiedGreen = Color(0xFF6DD58C),
    verifiedContainer = Color(0xFF163B22),
    onVerifiedContainer = Color(0xFFC4EED0),
    warningAmber = Color(0xFFFCC934),
    warningContainer = Color(0xFF3D2E05),
    onWarningContainer = Color(0xFFFFE082),
    criticalRed = Color(0xFFF28B82),
    criticalContainer = Color(0xFF4A1C19),
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
    primaryContainer = Color(0xFFE3EDFD),
    onPrimaryContainer = Color(0xFF041E49),
    secondary = Color(0xFF00639B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD6EEFF),
    onSecondaryContainer = Color(0xFF001D35),
    tertiary = Color(0xFF146C2E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD6F3DE),
    onTertiaryContainer = Color(0xFF072711),
    background = AccountPaletteTokens.LightCanvas,
    onBackground = AccountPaletteTokens.LightOnSurface,
    surface = AccountPaletteTokens.LightCanvas,
    onSurface = AccountPaletteTokens.LightOnSurface,
    surfaceVariant = AccountPaletteTokens.LightSurfaceVariant,
    onSurfaceVariant = AccountPaletteTokens.LightOnSurfaceVariant,
    surfaceContainerLowest = AccountPaletteTokens.LightSurfaceLowest,
    surfaceContainerLow = AccountPaletteTokens.LightSurfaceLow,
    surfaceContainer = AccountPaletteTokens.LightSurfaceContainer,
    surfaceContainerHigh = AccountPaletteTokens.LightSurfaceHigh,
    surfaceContainerHighest = AccountPaletteTokens.LightSurfaceHighest,
    outline = AccountPaletteTokens.LightOutline,
    outlineVariant = AccountPaletteTokens.LightBorder,
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFCE8E6),
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
    tertiaryContainer = Color(0xFF163B22),
    onTertiaryContainer = Color(0xFFC4EED0),
    background = AccountPaletteTokens.DarkCanvas,
    onBackground = AccountPaletteTokens.DarkOnSurface,
    surface = AccountPaletteTokens.DarkCanvas,
    onSurface = AccountPaletteTokens.DarkOnSurface,
    surfaceVariant = AccountPaletteTokens.DarkSurfaceVariant,
    onSurfaceVariant = AccountPaletteTokens.DarkOnSurfaceVariant,
    surfaceContainerLowest = AccountPaletteTokens.DarkSurfaceLowest,
    surfaceContainerLow = AccountPaletteTokens.DarkSurfaceLow,
    surfaceContainer = AccountPaletteTokens.DarkSurfaceContainer,
    surfaceContainerHigh = AccountPaletteTokens.DarkSurfaceHigh,
    surfaceContainerHighest = AccountPaletteTokens.DarkSurfaceHighest,
    outline = AccountPaletteTokens.DarkOutline,
    outlineVariant = AccountPaletteTokens.DarkBorder,
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
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.25).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.15).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.5.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.5.sp,
        lineHeight = 20.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.5.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.5.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.3.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.5.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.3.sp,
    ),
)

val AccountsShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

@Composable
fun GothwadAccountsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) AccountsDarkColorScheme else AccountsLightColorScheme
    val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            val barColorInt = semanticColors.canvasBackground.toArgb()
            @Suppress("DEPRECATION")
            window.statusBarColor = barColorInt
            @Suppress("DEPRECATION")
            window.navigationBarColor = barColorInt
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalAccountColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AccountsTypography,
            shapes = AccountsShapes,
            content = content,
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
