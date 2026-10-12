package br.com.lote.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import br.com.lote.ui.theme.LocalReducedMotion
import br.com.lote.ui.theme.Motion

/**
 * How much a pressed thing gives. A control sinks to 96%; a surface the size of a photograph
 * sinks to 98%, because the same 4% on a 256dp tile reads as a lurch rather than a press.
 */
enum class Press(val scale: Float) {
    Control(0.96f),
    Surface(0.98f),
}

// Every control answers a press the same way: a little smaller and a touch lighter, so
// the press is felt on a surface that has no ripple of its own. Going in takes 100 ms
// and starts under the finger; letting go takes 150 ms. With reduced motion only the
// lightening stays: it says "pressed" without moving anything. The values are read
// inside the layer block, so a press redraws the control without recomposing it.
@Composable
fun Modifier.pressFeedback(source: InteractionSource, press: Press = Press.Control): Modifier {
    val pressed by source.collectIsPressedAsState()
    val moves = !LocalReducedMotion.current
    val progress = animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = if (pressed) tween(Motion.PRESS_IN, easing = Motion.Enter) else tween(Motion.PRESS_OUT, easing = Motion.Standard),
        label = "press",
    )
    return graphicsLayer {
        val p = progress.value
        if (moves) {
            val scale = 1f - (1f - press.scale) * p
            scaleX = scale
            scaleY = scale
        }
        alpha = 1f - 0.15f * p
    }
}

/**
 * Keeps a control's 48dp touch target without paying 48dp of layout for it: the control is
 * measured at full size and centred in a box reporting at most [layoutHeight], so it overflows
 * equally above and below. Compose still delivers touches that land in the overflow.
 */
fun Modifier.touchOverflow(layoutHeight: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val height = minOf(placeable.height, layoutHeight.roundToPx())
    layout(placeable.width, height) { placeable.placeRelative(0, (height - placeable.height) / 2) }
}

/** A clickable that carries its role, its disabled state and the shared press response. */
@Composable
fun Modifier.pressClickable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    role: Role? = Role.Button,
    onClickLabel: String? = null,
    press: Press = Press.Control,
): Modifier {
    val source = remember { MutableInteractionSource() }
    return pressFeedback(source, press).clickable(
        interactionSource = source,
        indication = null,
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        onClick = onClick,
    )
}

/** The selectable twin of [pressClickable]: it exposes `selected` to accessibility services. */
@Composable
fun Modifier.pressSelectable(
    selected: Boolean,
    onClick: () -> Unit,
    role: Role? = Role.RadioButton,
): Modifier {
    val source = remember { MutableInteractionSource() }
    return pressFeedback(source).selectable(
        selected = selected,
        interactionSource = source,
        indication = null,
        role = role,
        onClick = onClick,
    )
}
