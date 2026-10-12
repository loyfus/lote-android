package br.com.lote.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import br.com.lote.ui.themeBarIcons

// The Material components that stay (navigation bar, menu, snackbar, sheet) read their
// colours from this scheme, so it is built from the same roles every other surface
// uses rather than restated: a role changes in one place.
private fun scheme(roles: Roles, opposite: Roles, dark: Boolean): ColorScheme {
    // The Material accent is the amber that can stand on paper (amberText): a switch,
    // a cursor or a radio drawn in the fill amber would measure under 2:1 on the ground.
    val base = if (dark) {
        darkColorScheme(
            primary = roles.amberText,
            onPrimary = roles.onAmber,
            secondary = roles.pine,
            onSecondary = Color(0xFF06301F),
        )
    } else {
        lightColorScheme(
            primary = roles.amberText,
            onPrimary = Color.White,
            secondary = roles.pine,
            onSecondary = Color.White,
        )
    }
    return base.copy(
        primaryContainer = roles.amberSoft,
        onPrimaryContainer = roles.amberDeep,
        secondaryContainer = roles.pineSoft,
        onSecondaryContainer = roles.pine,
        tertiary = roles.slate,
        onTertiary = roles.paper,
        tertiaryContainer = roles.slateSoft,
        onTertiaryContainer = roles.slate,
        background = roles.paper,
        onBackground = roles.ink,
        surface = roles.surface,
        onSurface = roles.ink,
        surfaceVariant = roles.paperDim,
        onSurfaceVariant = roles.ink60,
        // Tonal elevation would tint a menu toward the accent; surfaces here are
        // flat, so the tint is the surface itself.
        surfaceTint = roles.surface,
        inverseSurface = roles.ink,
        inverseOnSurface = roles.paper,
        // The accent on an ink-coloured snackbar is the other theme's accent: the
        // snackbar is dark on a light screen, so it needs the lifted amber.
        inversePrimary = opposite.amberText,
        error = roles.amberText,
        onError = if (dark) roles.onAmber else Color.White,
        outline = roles.lineControl,
        outlineVariant = roles.line,
        scrim = Color.Black,
        surfaceBright = roles.surface,
        surfaceDim = roles.paperDim,
        surfaceContainerLowest = roles.surface,
        surfaceContainerLow = roles.paper,
        surfaceContainer = roles.surface,
        surfaceContainerHigh = roles.surface,
        surfaceContainerHighest = roles.paperDim,
    )
}

private val LightScheme = scheme(LightRoles, DarkRoles, dark = false)
private val DarkScheme = scheme(DarkRoles, LightRoles, dark = true)

@Composable
fun LoteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkScheme else LightScheme
    val view = LocalView.current
    val context = LocalContext.current

    if (!view.isInEditMode) {
        SideEffect {
            // The window already draws edge to edge (enableEdgeToEdge in the
            // Activity), so only the icon tint follows the theme; setting the bar
            // colours here is both redundant and deprecated.
            val window = (context as? Activity)?.window ?: return@SideEffect
            themeBarIcons(WindowCompat.getInsetsController(window, view), darkTheme)
        }
    }

    // Read once per activity: what changes at run time is the animator scale, which
    // Compose applies to every duration by itself.
    val reducedMotion = remember(context) { context.prefersReducedMotion() }

    CompositionLocalProvider(
        LocalRoles provides if (darkTheme) DarkRoles else LightRoles,
        LocalReducedMotion provides reducedMotion,
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = LoteTypography,
            shapes = LoteShapes,
            content = content,
        )
    }
}
