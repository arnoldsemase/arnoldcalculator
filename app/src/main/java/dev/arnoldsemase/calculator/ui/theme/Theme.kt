package dev.arnoldsemase.calculator.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import dev.arnoldsemase.calculator.data.ThemeMode

/** Ember brand tokens. */
@Immutable
class ArnoldColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val outline: Color,
    val text: Color,
    val textSecondary: Color,
    val accent: Color,
    val accentSecondary: Color,
    /** Coral variant safe to use for small text on this theme's surfaces. */
    val accentText: Color,
    val onAccent: Color,
) {
    /** Surface tinted with the coral accent; used for operator keys and selected states. */
    fun accentTint(amount: Float = 0.16f): Color = lerp(surface, accent, amount)
}

val DarkColors = ArnoldColors(
    isDark = true,
    background = Color(0xFF0F0F0F),
    surface = Color(0xFF191919),
    surfaceHigh = Color(0xFF262626),
    outline = Color(0xFF2E2E2E),
    text = Color(0xFFF5F5F5),
    textSecondary = Color(0xFFA0A0A0),
    accent = Color(0xFFFF4C60),
    accentSecondary = Color(0xFFFF6677),
    accentText = Color(0xFFFF4C60),
    onAccent = Color(0xFF0F0F0F),
)

val LightColors = ArnoldColors(
    isDark = false,
    background = Color(0xFFF6F7FB),
    surface = Color(0xFFFFFFFF),
    surfaceHigh = Color(0xFFE9EBF2),
    outline = Color(0xFFDADDE6),
    text = Color(0xFF14151A),
    textSecondary = Color(0xFF5E6270),
    accent = Color(0xFFFF4C60),
    accentSecondary = Color(0xFFFF6677),
    accentText = Color(0xFFD42B45),
    onAccent = Color(0xFF0F0F0F),
)

val LocalArnoldColors = staticCompositionLocalOf { DarkColors }

object Arnold {
    val colors: ArnoldColors
        @Composable @ReadOnlyComposable get() = LocalArnoldColors.current
}

@Composable
fun ArnoldTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val c = if (dark) DarkColors else LightColors
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.accent, onPrimary = c.onAccent, background = c.background, onBackground = c.text,
            surface = c.surface, onSurface = c.text, surfaceVariant = c.surfaceHigh, onSurfaceVariant = c.textSecondary,
            outline = c.outline,
        )
    } else {
        lightColorScheme(
            primary = c.accent, onPrimary = c.onAccent, background = c.background, onBackground = c.text,
            surface = c.surface, onSurface = c.text, surfaceVariant = c.surfaceHigh, onSurfaceVariant = c.textSecondary,
            outline = c.outline,
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }

    CompositionLocalProvider(LocalArnoldColors provides c) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
