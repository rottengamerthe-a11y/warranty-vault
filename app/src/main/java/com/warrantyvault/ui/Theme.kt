package com.warrantyvault.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// CompositionLocal to thread the active theme through the composable tree
val LocalThemeConfig = staticCompositionLocalOf { ThemeConfig.Vault }

private val CleanSans = FontFamily.SansSerif

private val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = CleanSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 40.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = CleanSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 36.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = CleanSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = CleanSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = CleanSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = CleanSans,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontFamily = CleanSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = CleanSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = CleanSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = CleanSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = CleanSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = CleanSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = CleanSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp
    )
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun WarrantyVaultTheme(
    themeConfig: ThemeConfig = ThemeConfig.Vault,
    useDynamicColor: Boolean = false,
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    // Update the mutable color globals so existing screen code picking up VaultNight etc works
    applyThemeColors(themeConfig)

    val materialColors = darkColorScheme(
        primary = themeConfig.materialPrimary,
        onPrimary = themeConfig.materialOnPrimary,
        primaryContainer = themeConfig.materialPrimaryContainer,
        onPrimaryContainer = themeConfig.materialOnPrimaryContainer,
        secondary = themeConfig.materialSecondary,
        onSecondary = themeConfig.materialOnSecondary,
        secondaryContainer = themeConfig.materialSecondaryContainer,
        onSecondaryContainer = themeConfig.materialOnSecondaryContainer,
        tertiary = themeConfig.materialTertiary,
        onTertiary = themeConfig.materialOnTertiary,
        tertiaryContainer = themeConfig.materialTertiaryContainer,
        onTertiaryContainer = themeConfig.materialOnTertiaryContainer,
        background = themeConfig.night,
        onBackground = themeConfig.text,
        surface = themeConfig.materialSurface,
        onSurface = themeConfig.materialOnSurface,
        surfaceVariant = themeConfig.materialSurfaceVariant,
        onSurfaceVariant = themeConfig.materialOnSurfaceVariant,
        outline = themeConfig.materialOutline
    )

    CompositionLocalProvider(LocalThemeConfig provides themeConfig) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}

/** Copies the current theme config into the mutable globals in Color.kt so all existing
 *  screen code that references VaultNight, VaultMint etc picks them up. */
private fun applyThemeColors(t: ThemeConfig) {
    _VaultNight = t.night
    _VaultNightRaised = t.nightRaised
    _VaultGlass = t.glass
    _VaultGlassStrong = t.glassStrong
    _VaultGlassBorder = t.glassBorder
    _VaultMint = t.accent
    _VaultSky = t.accentSecondary
    _VaultCoral = if (t == ThemeConfig.Ember) t.accent else t.accentTertiary
    _VaultAmber = if (t == ThemeConfig.Ember) t.accentSecondary else t.accentTertiary
    _VaultText = t.text
    _VaultTextMuted = t.textMuted
}
