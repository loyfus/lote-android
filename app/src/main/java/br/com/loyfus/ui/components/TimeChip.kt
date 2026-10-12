package br.com.loyfus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.loyfus.ui.theme.OnAmber
import br.com.loyfus.ui.theme.Scrim
import br.com.loyfus.ui.theme.Type
import br.com.loyfus.ui.theme.Amber
import br.com.loyfus.ui.theme.sized

/**
 * The deadline badge on a photograph. Only the exceptional earns colour: an auction in its last
 * day wears the amber, everything else sits on the scrim — urgency is colour, clock and words
 * together, never colour alone. Decorative to a screen reader, since the card already says the
 * time. The height is a minimum, so the pill grows with its text at a large font scale.
 */
@Composable
fun TimeChip(spec: ChipSpec, modifier: Modifier = Modifier) {
    val background = if (spec.hot) Amber else Scrim
    val content = if (spec.hot) OnAmber else Color.White
    Row(
        modifier = modifier
            .clearAndSetSemantics { }
            .defaultMinSize(minHeight = 26.dp)
            .clip(CircleShape)
            .background(background)
            .padding(start = if (spec.clock) 8.dp else 10.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (spec.clock) {
            Icon(Icons.Outlined.Schedule, contentDescription = null, tint = content, modifier = Modifier.size(15.dp))
        }
        Text(
            text = spec.text,
            style = Type.caption.sized(12.5, 16.0, FontWeight.SemiBold),
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
