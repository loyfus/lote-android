package br.com.loyfus.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** One screen's wish for the bar icons. Identity, not equality: two equal wishes are two owners. */
private class BarIconRequest(val whiteStatus: Boolean, val whiteNavigation: Boolean)

/**
 * The wishes of every screen in the composition, oldest first. The newest one wins.
 *
 * A stack rather than "set on enter, restore on leave": during a navigation both screens
 * are composed, and the one leaving disposes after the one arriving has started. Home
 * asks for white icons on its night-blue bar; the lot's photograph asks for white too,
 * then for the theme's once its bar collapses. Restoring "what it was before" from Home
 * as it leaves would paint dark icons over the arriving photograph.
 */
private val requests = mutableListOf<BarIconRequest>()

/** The theme's own setting: what the icons are when no screen wishes otherwise. */
private var themeLightStatus = true
private var themeLightNavigation = true

/**
 * The theme's say: dark icons on a light theme, white on a dark one, unless a screen in
 * the composition wishes otherwise. The theme calls this instead of setting the bars
 * itself, because its effect runs after the screens' on the first frame and would
 * otherwise undo Home's white icons the moment the app opens.
 */
fun themeBarIcons(controller: WindowInsetsControllerCompat, darkTheme: Boolean) {
    themeLightStatus = !darkTheme
    themeLightNavigation = !darkTheme
    controller.applyNewest()
}

private fun WindowInsetsControllerCompat.applyNewest() {
    val newest = requests.lastOrNull()
    isAppearanceLightStatusBars = if (newest?.whiteStatus == true) false else themeLightStatus
    isAppearanceLightNavigationBars = if (newest?.whiteNavigation == true) false else themeLightNavigation
}

/**
 * White system bar icons for as long as this is in the composition, for a screen whose
 * ground under the bars is a photograph, black, or the night-blue bar of Home. When it
 * leaves, the next newest screen's wish applies, or the theme's when there is none.
 */
@Composable
fun SystemBarIcons(whiteStatus: Boolean, whiteNavigation: Boolean = false) {
    val context = LocalContext.current
    val view = LocalView.current
    DisposableEffect(whiteStatus, whiteNavigation) {
        val window = context.findActivity()?.window
        if (window == null) {
            onDispose { }
        } else {
            val controller = WindowCompat.getInsetsController(window, view)
            val request = BarIconRequest(whiteStatus, whiteNavigation)
            requests += request
            controller.applyNewest()
            onDispose {
                requests -= request
                controller.applyNewest()
            }
        }
    }
}
