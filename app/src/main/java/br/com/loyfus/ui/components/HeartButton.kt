package br.com.loyfus.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.loyfus.ui.theme.HeartOn
import br.com.loyfus.ui.theme.LocalReducedMotion
import br.com.loyfus.ui.theme.Motion
import br.com.loyfus.ui.theme.Scrim
import br.com.loyfus.ui.theme.Touch
import kotlinx.coroutines.launch

/**
 * The favourite toggle on a photograph: off is a scrim disc with an outlined heart, on a white
 * disc with a filled one — the state is the shape, never only the colour. The visible disc is
 * [size] and the touch target is always 48dp around it. Turning it pops the heart (1 to 1.25 to
 * 1) with a haptic tick, unless reduced motion is on, and then the heart just changes.
 *
 * [title] is the lot's title: "Favoritar X" says what the control will do to which lot, which
 * "Favoritar" alone, repeated down a list, cannot.
 */
@Composable
fun HeartButton(
    on: Boolean,
    title: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    iconSize: Dp = 20.dp,
) {
    val reducedMotion = LocalReducedMotion.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val pop = remember { Animatable(1f) }
    val label = if (on) "Remover dos favoritos: $title" else "Favoritar $title"

    // The disc turns white and the heart amber in 120 ms, under the pop: the shape
    // (outlined or filled) is what carries the state, the colour only follows it.
    val disc by animateColorAsState(if (on) Color.White else Scrim, Motion.fast(), label = "heartDisc")
    val tint by animateColorAsState(if (on) HeartOn else Color.White, Motion.fast(), label = "heartTint")
    Box(
        modifier = modifier
            .size(Touch.target)
            .pressClickable(
                onClick = {
                    haptics.performHapticFeedback(if (on) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
                    if (!reducedMotion) {
                        scope.launch {
                            pop.animateTo(1.25f, tween(Motion.POP / 2, easing = Motion.Enter))
                            pop.animateTo(1f, tween(Motion.POP / 2, easing = Motion.Standard))
                        }
                    }
                    onToggle()
                },
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                }
                .clip(CircleShape)
                .background(disc),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (on) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

