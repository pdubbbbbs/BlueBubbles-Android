package com.bluebubbles.messaging.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val BlueBubblesColorScheme = darkColorScheme(
  primary = CyanPrimary,
  onPrimary = BackgroundDark,
  primaryContainer = CyanDark,
  onPrimaryContainer = TextPrimary,

  secondary = PurplePrimary,
  onSecondary = TextPrimary,
  secondaryContainer = PurpleDark,
  onSecondaryContainer = TextPrimary,

  tertiary = GreenAccent,
  onTertiary = BackgroundDark,

  background = BackgroundDark,
  onBackground = TextPrimary,

  surface = SurfaceDark,
  onSurface = TextPrimary,
  surfaceVariant = CardBackground,
  onSurfaceVariant = TextSecondary,

  error = RedAccent,
  onError = TextPrimary,

  outline = PurpleDark,
  outlineVariant = Color(0xFF2A2A2D)
)

// Gradient definitions
object BlueBubblesGradients {
  val backgroundGradient = Brush.verticalGradient(
    colors = listOf(BackgroundDark, BackgroundPurple, BackgroundDark)
  )

  val cyanPurpleGradient = Brush.linearGradient(
    colors = listOf(CyanPrimary, PurplePrimary)
  )

  val cardGlowGradient = Brush.radialGradient(
    colors = listOf(
      CyanPrimary.copy(alpha = 0.2f),
      Color.Transparent
    )
  )

  val headerGradient = Brush.horizontalGradient(
    colors = listOf(CyanPrimary, PurpleLight)
  )
}

@Composable
fun BlueBubblesTheme(
  content: @Composable () -> Unit
) {
  val colorScheme = BlueBubblesColorScheme
  val view = LocalView.current

  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window
      window.statusBarColor = BackgroundDark.toArgb()
      window.navigationBarColor = BackgroundDark.toArgb()
      WindowCompat.getInsetsController(window, view).apply {
        isAppearanceLightStatusBars = false
        isAppearanceLightNavigationBars = false
      }
    }
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = BlueBubblesTypography,
    content = content
  )
}
