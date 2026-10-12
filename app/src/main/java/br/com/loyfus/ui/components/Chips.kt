package br.com.loyfus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.loyfus.ui.theme.Corners
import br.com.loyfus.ui.theme.Ink
import br.com.loyfus.ui.theme.Ink60
import br.com.loyfus.ui.theme.Ink80
import br.com.loyfus.ui.theme.LineControl
import br.com.loyfus.ui.theme.Motion
import br.com.loyfus.ui.theme.Paper
import br.com.loyfus.ui.theme.PaperDim
import br.com.loyfus.ui.theme.Touch
import br.com.loyfus.ui.theme.Type
import br.com.loyfus.ui.theme.sized

/**
 * A choice among a few: category, type, deadline, sort. A control, so it has a border
 * (1dp, the 3:1 line of a control); selected, it fills with ink and loses the border.
 * The pill is 40dp tall inside a 48dp touch target, and it reports `selected` to
 * accessibility services: a chip that only changes colour says nothing to a reader
 * who cannot see it.
 */
@Composable
fun Chip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    /** 38 for the budget shortcuts on Home, 40 everywhere else. */
    height: Dp = 40.dp,
    horizontalPadding: Dp = 14.dp,
    labelStyle: TextStyle = Type.label,
) {
    // Selecting fills the pill with ink in 120 ms instead of swapping it: the eye sees
    // which chip took the choice. The border turns ink with the fill, so it disappears
    // into it rather than being removed, and nothing in the layout moves.
    val ink = Ink
    val fill by animateColorAsState(if (selected) ink else ink.copy(alpha = 0f), Motion.fast(), label = "chipFill")
    val edge by animateColorAsState(if (selected) ink else LineControl, Motion.fast(), label = "chipEdge")
    val labelColor by animateColorAsState(if (selected) Paper else Ink, Motion.fast(), label = "chipLabel")
    val iconColor by animateColorAsState(if (selected) Paper else Ink60, Motion.fast(), label = "chipIcon")
    Box(
        modifier = modifier
            .touchOverflow(40.dp)
            .heightIn(min = Touch.target)
            .pressSelectable(selected = selected, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = height)
                .clip(CircleShape)
                .background(fill)
                .border(1.dp, edge, CircleShape)
                .padding(horizontal = horizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(text = label, style = labelStyle, color = labelColor, maxLines = 1)
        }
    }
}

/**
 * An active filter, with the cross that removes it. The whole tag is the button: the
 * cross is only the picture of what tapping does, so there is no 18dp target to miss.
 */
@Composable
fun RemovableTag(label: String, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .touchOverflow(32.dp)
            .heightIn(min = Touch.target)
            .pressClickable(onClick = onRemove)
            .semantics { contentDescription = "Remover filtro: $label" },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 32.dp)
                .clip(Corners.tag)
                .background(PaperDim)
                .padding(start = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = Type.label.sized(13.0, 18.0),
                color = Ink80,
                maxLines = 1,
            )
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = null,
                tint = Ink60,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
