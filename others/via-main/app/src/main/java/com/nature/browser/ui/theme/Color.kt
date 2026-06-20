package com.nature.browser.ui.theme

import androidx.compose.ui.graphics.Color

// Nature Theme - Primary Palette
val RiverTeal = Color(0xFF2A9D8F)
val SkyBlue = Color(0xFF48CAE4)
val WillowGreen = Color(0xFF57CC99)
val CloudWhite = Color(0xFFF0FAF8)
val StormGrey = Color(0xFF264653)
val ClearStreamBg = Color(0xFFF5F9F5)

// Material 3 colors derived from palette
val LightPrimary = RiverTeal
val LightOnPrimary = Color.White
val LightPrimaryContainer = WillowGreen.copy(alpha = 0.2f)
val LightOnPrimaryContainer = StormGrey

val LightSecondary = SkyBlue
val LightOnSecondary = Color.White
val LightSecondaryContainer = SkyBlue.copy(alpha = 0.2f)
val LightOnSecondaryContainer = StormGrey

val LightBackground = ClearStreamBg
val LightOnBackground = StormGrey
val LightSurface = Color.White
val LightOnSurface = StormGrey

// Dark Theme - Canopy
val CanopyBg = Color(0xFF0D211F)
val CanopyPrimary = WillowGreen
val CanopySecondary = RiverTeal

// Dark Theme - Twilight Forest
val TwilightBg = Color(0xFF1A1423)
val TwilightPrimary = Color(0xFF7B2CBF)
val TwilightSecondary = RiverTeal

// Coastal
val CoastalBg = Color(0xFFF1FAEE)
val CoastalPrimary = Color(0xFF457B9D)

// High Alpine
val AlpineBg = Color(0xFFE5E5E5)
val AlpinePrimary = Color(0xFF495057)

// Legacy colors (keep for compatibility if needed, but we will mostly use the new ones)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
