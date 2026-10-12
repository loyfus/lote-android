package br.com.loyfus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.com.loyfus.ui.theme.Corners
import br.com.loyfus.ui.theme.Ink
import br.com.loyfus.ui.theme.LineControl
import br.com.loyfus.ui.theme.OnAmber
import br.com.loyfus.ui.theme.Scrim
import br.com.loyfus.ui.theme.Touch
import br.com.loyfus.ui.theme.Type
import br.com.loyfus.ui.theme.Amber

// Three emphases and one rule: every button keeps a 48dp hit box at least, answers a press (a
// hair smaller, a touch lighter), and says so when disabled (45%, reaching accessibility
// services). The look is the specification's:
//   primary    52dp, radius 14, amber with the on-amber label, icon at the end
//   secondary  the same box, a 1dp control border, ink label
//   quiet      a label and nothing else, in ink: it is not the brand's colour

@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) = BoxButton(label, onClick, modifier, icon, enabled, background = Amber, content = OnAmber, border = false)

@Composable
fun SecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) = BoxButton(label, onClick, modifier, icon, enabled, background = Color.Transparent, content = Ink, border = true)

@Composable
private fun BoxButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier,
    icon: ImageVector?,
    enabled: Boolean,
    background: Color,
    content: Color,
    border: Boolean,
) {
    val shape: Shape = Corners.field
    Row(
        modifier = modifier
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .pressClickable(onClick = onClick, enabled = enabled)
            .defaultMinSize(minHeight = 52.dp)
            .clip(shape)
            .background(background)
            .then(if (border) Modifier.border(1.dp, LineControl, shape) else Modifier)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        Text(text = label, style = Type.button, color = content, textAlign = TextAlign.Center)
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
        }
    }
}

/**
 * A label that acts: "Limpar", "Ver todos", "Ler mais", "Desfazer". Ink on nothing. It
 * takes 36dp of layout (a line of text and its air) and keeps a 48dp touch target.
 */
@Composable
fun QuietButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .touchOverflow(36.dp)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .pressClickable(onClick = onClick, enabled = enabled)
            .defaultMinSize(minHeight = Touch.target)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = label, style = Type.label, color = Ink)
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = Ink, modifier = Modifier.size(20.dp))
        }
    }
}

/**
 * The round control on a photograph: back, share, close. A 42dp scrim disc inside a
 * 48dp target, white glyph. [label] is what a screen reader says.
 */
@Composable
fun PhotoIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    disc: Color = Scrim,
) {
    Box(
        modifier = modifier
            .size(Touch.target)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .pressClickable(onClick = onClick, enabled = enabled)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(CircleShape).background(disc),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
    }
}

private const val DISABLED_ALPHA = 0.45f
