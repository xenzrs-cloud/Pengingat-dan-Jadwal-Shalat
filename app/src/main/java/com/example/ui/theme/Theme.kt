package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.domain.WidgetThemeMode
import com.example.widget.DynamicDayPhase

private val DefaultDarkColorScheme =
  darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = EmeraldOnPrimaryDark,
    primaryContainer = EmeraldPrimaryContainerDark,
    onPrimaryContainer = EmeraldOnPrimaryContainerDark,
    secondary = GoldSecondaryDark,
    onSecondary = GoldOnSecondaryDark,
    secondaryContainer = GoldSecondaryContainerDark,
    onSecondaryContainer = GoldOnSecondaryContainerDark,
    tertiary = TealTertiaryDark,
    onTertiary = TealOnTertiaryDark,
    tertiaryContainer = TealTertiaryContainerDark,
    onTertiaryContainer = TealOnTertiaryContainerDark,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = ErrorDark,
    errorContainer = ErrorContainerDark,
  )

private val DefaultLightColorScheme =
  lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldPrimaryContainer,
    onPrimaryContainer = EmeraldOnPrimaryContainer,
    secondary = GoldSecondary,
    onSecondary = GoldOnSecondary,
    secondaryContainer = GoldSecondaryContainer,
    onSecondaryContainer = GoldOnSecondaryContainer,
    tertiary = TealTertiary,
    onTertiary = TealOnTertiary,
    tertiaryContainer = TealTertiaryContainer,
    onTertiaryContainer = TealOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = ErrorLight,
    errorContainer = ErrorContainerLight,
  )

// Bright Daytime Mosque Palette (Sky Blue & Fresh Emerald)
private val DayMosqueLightColorScheme =
  lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0C4A6E),
    secondary = Color(0xFF0F766E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF042F2E),
    tertiary = Color(0xFF0D9488),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF99F6E4),
    onTertiaryContainer = Color(0xFF134E4A),
    background = Color(0xFFEBF8FF),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE0F2FE),
    onSurfaceVariant = Color(0xFF334155),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFBAE6FD),
    error = ErrorLight,
    errorContainer = ErrorContainerLight,
  )

// Dawn Mosque Palette (Deep Indigo-Violet & Rose Gold)
private val DawnMosqueLightColorScheme =
  lightColorScheme(
    primary = Color(0xFF5B21B6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF2E1065),
    secondary = Color(0xFFDB2777),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFCE7F3),
    onSecondaryContainer = Color(0xFF500724),
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF451A03),
    background = Color(0xFFF5F3FF),
    onBackground = Color(0xFF1E1B4B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1E1B4B),
    surfaceVariant = Color(0xFFEDE9FE),
    onSurfaceVariant = Color(0xFF4C1D95),
    outline = Color(0xFFC4B5FD),
    outlineVariant = Color(0xFFDDD6FE),
    error = ErrorLight,
    errorContainer = ErrorContainerLight,
  )

private val DawnMosqueDarkColorScheme =
  darkColorScheme(
    primary = Color(0xFFA78BFA),
    onPrimary = Color(0xFF2E1065),
    primaryContainer = Color(0xFF4C1D95),
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = Color(0xFFF472B6),
    onSecondary = Color(0xFF500724),
    secondaryContainer = Color(0xFF831843),
    onSecondaryContainer = Color(0xFFFCE7F3),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = Color(0xFF131026),
    onBackground = Color(0xFFEDE9FE),
    surface = Color(0xFF1D1838),
    onSurface = Color(0xFFF5F3FF),
    surfaceVariant = Color(0xFF2A224F),
    onSurfaceVariant = Color(0xFFC4B5FD),
    outline = Color(0xFF4C1D95),
    outlineVariant = Color(0xFF312E81),
    error = ErrorDark,
    errorContainer = ErrorContainerDark,
  )

// Sunset Mosque Palette (Warm Amber Orange & Magenta Plum)
private val SunsetMosqueLightColorScheme =
  lightColorScheme(
    primary = Color(0xFFEA580C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEDD5),
    onPrimaryContainer = Color(0xFF431407),
    secondary = Color(0xFFBE185D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFCE7F3),
    onSecondaryContainer = Color(0xFF500724),
    tertiary = Color(0xFF7C3AED),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDE9FE),
    onTertiaryContainer = Color(0xFF2E1065),
    background = Color(0xFFFFF7ED),
    onBackground = Color(0xFF27140E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF27140E),
    surfaceVariant = Color(0xFFFFEDD5),
    onSurfaceVariant = Color(0xFF7C2D12),
    outline = Color(0xFFFDBA74),
    outlineVariant = Color(0xFFFED7AA),
    error = ErrorLight,
    errorContainer = ErrorContainerLight,
  )

private val SunsetMosqueDarkColorScheme =
  darkColorScheme(
    primary = Color(0xFFFB923C),
    onPrimary = Color(0xFF431407),
    primaryContainer = Color(0xFF9A3412),
    onPrimaryContainer = Color(0xFFFFEDD5),
    secondary = Color(0xFFF472B6),
    onSecondary = Color(0xFF500724),
    secondaryContainer = Color(0xFF831843),
    onSecondaryContainer = Color(0xFFFCE7F3),
    tertiary = Color(0xFFC084FC),
    onTertiary = Color(0xFF3B0764),
    tertiaryContainer = Color(0xFF581C87),
    onTertiaryContainer = Color(0xFFF3E8FF),
    background = Color(0xFF1F1116),
    onBackground = Color(0xFFFFEDD5),
    surface = Color(0xFF2B1820),
    onSurface = Color(0xFFFFF7ED),
    surfaceVariant = Color(0xFF3D222D),
    onSurfaceVariant = Color(0xFFFDBA74),
    outline = Color(0xFF7C2D12),
    outlineVariant = Color(0xFF581C87),
    error = ErrorDark,
    errorContainer = ErrorContainerDark,
  )

// Starry Night Mosque Palette (Always Calm Deep Starry Night & Crescent Gold)
private val NightMosqueColorScheme =
  darkColorScheme(
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF042F2E),
    primaryContainer = Color(0xFF115E59),
    onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = Color(0xFFFACC15),
    onSecondary = Color(0xFF422006),
    secondaryContainer = Color(0xFF713F12),
    onSecondaryContainer = Color(0xFFFEF08A),
    tertiary = Color(0xFF38BDF8),
    onTertiary = Color(0xFF082F49),
    tertiaryContainer = Color(0xFF0C4A6E),
    onTertiaryContainer = Color(0xFFE0F2FE),
    background = Color(0xFF060E1F),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF0F1C34),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF172947),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF1E3A5F),
    error = ErrorDark,
    errorContainer = ErrorContainerDark,
  )

private val AppShapes =
  Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
  )

fun resolveColorScheme(
  darkTheme: Boolean,
  themeMode: WidgetThemeMode,
  dynamicPhase: DynamicDayPhase,
): ColorScheme {
  if (themeMode == WidgetThemeMode.DEFAULT) {
    return if (darkTheme) DefaultDarkColorScheme else DefaultLightColorScheme
  }

  val activePhase =
    when (themeMode) {
      WidgetThemeMode.DYNAMIC_MOSQUE -> dynamicPhase
      WidgetThemeMode.MOSQUE_DAY -> DynamicDayPhase.DAY
      WidgetThemeMode.MOSQUE_SUNSET -> DynamicDayPhase.SUNSET
      WidgetThemeMode.MOSQUE_NIGHT -> DynamicDayPhase.NIGHT
      WidgetThemeMode.DEFAULT -> DynamicDayPhase.DAY
    }

  return when (activePhase) {
    DynamicDayPhase.DAY ->
      if (darkTheme && themeMode == WidgetThemeMode.DEFAULT) DefaultDarkColorScheme
      else DayMosqueLightColorScheme
    DynamicDayPhase.DAWN ->
      if (darkTheme) DawnMosqueDarkColorScheme else DawnMosqueLightColorScheme
    DynamicDayPhase.SUNSET ->
      if (darkTheme) SunsetMosqueDarkColorScheme else SunsetMosqueLightColorScheme
    DynamicDayPhase.NIGHT -> NightMosqueColorScheme
  }
}

@Composable
fun PengingatShalatTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  themeMode: WidgetThemeMode = WidgetThemeMode.DEFAULT,
  dynamicPhase: DynamicDayPhase = DynamicDayPhase.DAY,
  content: @Composable () -> Unit,
) {
  val colorScheme = resolveColorScheme(darkTheme, themeMode, dynamicPhase)

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = AppShapes,
    content = content,
  )
}
