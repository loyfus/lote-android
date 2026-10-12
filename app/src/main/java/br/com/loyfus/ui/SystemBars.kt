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
 * The wishes of every screen in the composition, oldest first; the newest wins. A stack rather
 * than set/restore because during navigation both screens are composed and the one leaving
 * disposes after the one arriving has started: restoring "what it was before" from Home as it
 * leaves would paint dark icons over the arriving photograph.
 */
private val requests = mutableListOf<BarIconRequest>()

/** The theme's own setting: what the icons are when no screen wishes otherwise. */
private var themeLightStatus = true
private var themeLightNavigation = true

/**
 * The theme's say: dark icons on a light theme, white on a dark one, unless a screen wishes
 * otherwise. The theme calls this instead of setting the bars itself because its effect runs
 * after the screens' on the first frame, undoing Home's white icons the moment the app opens.
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

/** White system bar icons while this is composed. When it leaves, the next newest wish applies. */
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
