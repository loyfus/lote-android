package br.com.loyfus.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.staticCompositionLocalOf

// Motion budget. Only the necessary and never decorative: a press answers, content
// arrives, a state changes in place, a lot opens from where it was. Every duration
// here is short enough that nothing waits for it: the state changes at once and the
// motion only shows the change. Everything yields to the system's reduce-motion
// setting (see LocalReducedMotion).
object Motion {
    /** A press going in: the answer has to start under the finger. */
    const val PRESS_IN = 100

    /** A press letting go: a touch slower than going in, so a tap reads as one gesture. */
    const val PRESS_OUT = 150

    /** A state changing in place: chip, colour, badge, counter, icon. */
    const val FAST = 120

    /** Content arriving or leaving: photo fade-in, skeleton to content, expanding blocks, rows. */
    const val STANDARD = 200

    /**
     * A block closing up or opening and moving what is below it. On the standard curve,
     * not the front-loaded enter one: a layout that moves has to be seen moving.
     */
    const val COLLAPSE = 250

    /** Opening a lot and coming back from it. */
    const val SCREEN = 280

    /** Sheets and large-surface movement. */
    const val SLOW = 300

    /** The heart's pop: 1 to 1.25 to 1. */
    const val POP = 220

    /** Half a breath of a loading placeholder: 900 ms dimming, 900 ms back. */
    const val PULSE = 900

    /** Arriving: fast at first, settling at the end (Material's emphasized decelerate). */
    val Enter: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Leaving: gentle at first, then gone (Material's emphasized accelerate). */
    val Exit: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** A change in place, neither arriving nor leaving. */
    val Standard: Easing = FastOutSlowInEasing

    fun <T> fast(): TweenSpec<T> = tween(FAST, easing = Standard)
    fun <T> enter(duration: Int = STANDARD): TweenSpec<T> = tween(duration, easing = Enter)
    fun <T> exit(duration: Int = FAST): TweenSpec<T> = tween(duration, easing = Exit)
    fun <T> collapse(): TweenSpec<T> = tween(COLLAPSE, easing = Standard)

    /**
     * One state of a screen giving way to the next: skeleton to content, content to error.
     * The new one fades in over the standard time while the old one leaves faster, so the
     * two never sit on top of each other long enough to read as a double exposure.
     */
    val contentSwap: ContentTransform
        get() = ContentTransform(
            targetContentEnter = fadeIn(enter()),
            initialContentExit = fadeOut(exit()),
            sizeTransform = SizeTransform(clip = false) { _, _ -> enter() },
        )

    /**
     * A number changing in place: the new one rises in when the count goes up and drops
     * in when it goes down, a quarter of its height, in 120 ms. With reduced motion it
     * only fades.
     */
    fun countSwap(reduced: Boolean, up: Boolean): ContentTransform {
        val inFade = fadeIn(fast())
        val outFade = fadeOut(fast())
        val size = SizeTransform(clip = false)
        if (reduced) return ContentTransform(inFade, outFade, sizeTransform = size)
        val sign = if (up) 1 else -1
        return ContentTransform(
            targetContentEnter = inFade + slideInVertically(fast()) { sign * it / 4 },
            initialContentExit = outFade + slideOutVertically(fast()) { -sign * it / 4 },
            sizeTransform = size,
        )
    }

    /** A badge appearing: it grows from 60% as it fades in. With reduced motion, only the fade. */
    fun badgeIn(reduced: Boolean): EnterTransition =
        if (reduced) fadeIn(fast()) else fadeIn(fast()) + scaleIn(enter(FAST), initialScale = 0.6f)

    fun badgeOut(reduced: Boolean): ExitTransition =
        if (reduced) fadeOut(fast()) else fadeOut(fast()) + scaleOut(exit(), targetScale = 0.6f)
}

/**
 * True when the system asks for no animation. An animator duration scale of zero is
 * what both the developer option and the accessibility "remove animations" setting
 * write, so it is the reduce-motion signal on Android.
 */
fun Context.prefersReducedMotion(): Boolean = runCatching {
    Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
}.getOrDefault(false)

/**
 * Whether movement through space is off. Compose already shortens every duration by the
 * system's animator scale, so a fade simply completes at once; this is for what is not
 * a duration — a slide, a scale, a pop, a pulse — which is left out entirely rather than
 * squeezed to nothing.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }
