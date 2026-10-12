package br.com.loyfus.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import br.com.loyfus.ui.theme.LocalReducedMotion
import br.com.loyfus.ui.theme.Motion

/**
 * A count that changes in place — a badge, a total — drawn by [content]. The new value
 * comes in from below when it grows and from above when it shrinks (Motion.countSwap),
 * so the direction of the change is visible as well as the number. Only a change of
 * value animates; recomposing with the same value does nothing. `null` is "not known
 * yet" and swaps by fading.
 */
@Composable
fun AnimatedCount(
    value: Long?,
    modifier: Modifier = Modifier,
    content: @Composable (Long?) -> Unit,
) {
    val reduced = LocalReducedMotion.current
    AnimatedContent(
        targetState = value,
        modifier = modifier,
        contentAlignment = Alignment.CenterStart,
        transitionSpec = {
            val from = initialState
            val to = targetState
            Motion.countSwap(reduced = reduced || from == null || to == null, up = from != null && to != null && to > from)
        },
        label = "count",
    ) { content(it) }
}

/**
 * A count badge that appears and disappears instead of blinking: it grows in when the
 * count leaves zero, shrinks out when it returns to zero (still showing the last number
 * as it goes), and cross-fades between two non-zero counts. [badge] draws one count.
 */
@Composable
fun AnimatedBadge(
    count: Int,
    modifier: Modifier = Modifier,
    badge: @Composable (Int) -> Unit,
) {
    val reduced = LocalReducedMotion.current
    AnimatedContent(
        targetState = count,
        modifier = modifier,
        contentAlignment = Alignment.Center,
        transitionSpec = {
            when {
                initialState == 0 -> Motion.badgeIn(reduced) togetherWith ExitTransition.None
                targetState == 0 -> EnterTransition.None togetherWith Motion.badgeOut(reduced)
                else -> fadeIn(Motion.fast()) togetherWith fadeOut(Motion.fast())
            }.using(SizeTransform(clip = false))
        },
        label = "badge",
    ) { n -> if (n > 0) badge(n) }
}
