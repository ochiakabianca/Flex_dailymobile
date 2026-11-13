package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class FlexAccentColors(
  val volt: Color,
  val hydration: Color,
  val energy: Color,
  val trackBg: Color,
  val cardElevated: Color,
)

val LocalFlexColors = staticCompositionLocalOf {
  FlexAccentColors(
    volt = VoltLime,
    hydration = HydrationCyan,
    energy = EnergyCoral,
    trackBg = SlateSurfaceHigh,
    cardElevated = SlateSurfaceVariant,
  )
}

private val FlexDarkColorScheme = darkColorScheme(
  primary = VoltLime,
  onPrimary = OnVoltLime,
  primaryContainer = VoltLimeContainerDark,
  onPrimaryContainer = VoltLimeSoft,
  secondary = HydrationCyan,
  onSecondary = Color(0xFF002133),
  secondaryContainer = HydrationContainerDark,
  onSecondaryContainer = Color(0xFFBAE6FD),
  tertiary = EnergyCoral,
  onTertiary = Color(0xFF2B0700),
  tertiaryContainer = EnergyContainerDark,
  onTertiaryContainer = Color(0xFFFFDAD2),
  background = ObsidianBackground,
  onBackground = TextPrimaryDark,
  surface = SlateSurface,
  onSurface = TextPrimaryDark,
  surfaceVariant = SlateSurfaceVariant,
  onSurfaceVariant = TextSecondaryDark,
  outline = SlateOutline,
  outlineVariant = Color(0xFF222A38),
  error = ErrorRedDark,
  onError = Color(0xFF370003),
)

private val FlexLightColorScheme = lightColorScheme(
  primary = VoltLimeDark,
  onPrimary = Color.White,
  primaryContainer = VoltLimeContainerLight,
  onPrimaryContainer = Color(0xFF121F00),
  secondary = HydrationCyanDark,
  onSecondary = Color.White,
  secondaryContainer = HydrationContainerLight,
  onSecondaryContainer = Color(0xFF083959),
  tertiary = EnergyCoralDark,
  onTertiary = Color.White,
  tertiaryContainer = EnergyContainerLight,
  onTertiaryContainer = Color(0xFF5C1904),
  background = CloudBackground,
  onBackground = TextPrimaryLight,
  surface = PaperSurface,
  onSurface = TextPrimaryLight,
  surfaceVariant = SageSurfaceVariant,
  onSurfaceVariant = TextSecondaryLight,
  outline = SageOutline,
  outlineVariant = Color(0xFFDFE6D8),
  error = ErrorRedLight,
  onError = Color.White,
)

private val FlexShapes = Shapes(
  small = RoundedCornerShape(12.dp),
  medium = RoundedCornerShape(18.dp),
  large = RoundedCornerShape(24.dp),
  extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun FlexDailyTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) FlexDarkColorScheme else FlexLightColorScheme
  val flexColors = if (darkTheme) {
    FlexAccentColors(
      volt = VoltLime,
      hydration = HydrationCyan,
      energy = EnergyCoral,
      trackBg = SlateSurfaceHigh,
      cardElevated = SlateSurfaceVariant,
    )
  } else {
    FlexAccentColors(
      volt = VoltLimeDark,
      hydration = HydrationCyanDark,
      energy = EnergyCoralDark,
      trackBg = Color(0xFFE2E8DA),
      cardElevated = Color(0xFFF0F4EB),
    )
  }

  CompositionLocalProvider(LocalFlexColors provides flexColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = FlexTypography,
      shapes = FlexShapes,
      content = content,
    )
  }
}
