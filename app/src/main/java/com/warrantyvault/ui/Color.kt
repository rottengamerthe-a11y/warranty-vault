package com.warrantyvault.ui

import androidx.compose.ui.graphics.Color

// These are mutable backing fields so the theme engine (Theme.kt) can update them
// at runtime. Screen composables read the public vals, which point to these.
var _VaultNight = Color(0xFF121417)
var _VaultNightRaised = Color(0xFF1A1D22)
var _VaultGlass = Color(0x47262B33)
var _VaultGlassStrong = Color(0x66313843)
var _VaultGlassBorder = Color(0x33E7EDF4)
var _VaultMint = Color(0xFF86D6A6)
var _VaultCoral = Color(0xFFE49788)
var _VaultAmber = Color(0xFFE1BF73)
var _VaultSky = Color(0xFF85B9D8)
var _VaultText = Color(0xFFE9ECE8)
var _VaultTextMuted = Color(0xFFB7BDB5)

// Public convenience vals that all screens use
val VaultNight get() = _VaultNight
val VaultNightRaised get() = _VaultNightRaised
val VaultGlass get() = _VaultGlass
val VaultGlassStrong get() = _VaultGlassStrong
val VaultGlassBorder get() = _VaultGlassBorder
val VaultMint get() = _VaultMint
val VaultCoral get() = _VaultCoral
val VaultAmber get() = _VaultAmber
val VaultSky get() = _VaultSky
val VaultText get() = _VaultText
val VaultTextMuted get() = _VaultTextMuted

// Legacy unused vals kept for reference
val PantrySeed = Color(0xFF7CCF9B)

val LightPrimary = Color(0xFF386A20)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFB8F397)
val LightOnPrimaryContainer = Color(0xFF062100)
val LightSecondary = Color(0xFF55624C)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFD9E7CA)
val LightOnSecondaryContainer = Color(0xFF131F0D)
val LightTertiary = Color(0xFF386666)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFBCECEC)
val LightOnTertiaryContainer = Color(0xFF002020)
val LightBackground = Color(0xFFFBFDF6)
val LightOnBackground = Color(0xFF1A1C18)
val LightSurface = Color(0xFFFBFDF6)
val LightOnSurface = Color(0xFF1A1C18)
val LightSurfaceVariant = Color(0xFFE0E4D6)
val LightOnSurfaceVariant = Color(0xFF44483E)
val LightOutline = Color(0xFF74796D)

val DarkPrimary = Color(0xFF9DD67D)
val DarkOnPrimary = Color(0xFF0C3900)
val DarkPrimaryContainer = Color(0xFF205107)
val DarkOnPrimaryContainer = Color(0xFFB8F397)
val DarkSecondary = Color(0xFFBDCBB0)
val DarkOnSecondary = Color(0xFF273421)
val DarkSecondaryContainer = Color(0xFF3D4A36)
val DarkOnSecondaryContainer = Color(0xFFD9E7CA)
val DarkTertiary = Color(0xFFA0D0D0)
val DarkOnTertiary = Color(0xFF003737)
val DarkTertiaryContainer = Color(0xFF1E4E4E)
val DarkOnTertiaryContainer = Color(0xFFBCECEC)
val DarkBackground = Color(0xFF12140F)
val DarkOnBackground = Color(0xFFE3E3DB)
val DarkSurface = Color(0xFF12140F)
val DarkOnSurface = Color(0xFFE3E3DB)
val DarkSurfaceVariant = Color(0xFF44483E)
val DarkOnSurfaceVariant = Color(0xFFC4C8BA)
val DarkOutline = Color(0xFF8E9286)
