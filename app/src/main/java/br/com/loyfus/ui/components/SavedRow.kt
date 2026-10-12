package br.com.loyfus.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.loyfus.ui.theme.Corners
import br.com.loyfus.ui.theme.Ink
import br.com.loyfus.ui.theme.Ink60
import br.com.loyfus.ui.theme.Space
import br.com.loyfus.ui.theme.Touch
import br.com.loyfus.ui.theme.Type
import br.com.loyfus.ui.theme.AmberText
import br.com.loyfus.ui.theme.sized

/**
 * A saved lot as a row: photo, title, price, deadline and the filled heart that removes it. An
 * auction that is over is dimmed to 60%: still readable, visibly no longer in play. Like the
 * tile, the row is one button and the heart is a stop of its own beside it.
 */
@Composable
fun SavedRow(
    model: LotCardModel,
    dimmed: Boolean,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (dimmed) 0.6f else 1f)
            .padding(horizontal = Space.gutter, vertical = Space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .pressClickable(onClick = onOpen, press = Press.Surface)
                .clearAndSetSemantics { contentDescription = model.label },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PhotoPlate(
                url = model.lot.primaryImageUrl,
                modifier = Modifier.size(width = 104.dp, height = 78.dp),
                shape = Corners.control,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = model.title,
                    style = Type.cardTitle,
                    color = Ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = model.price, style = Type.priceRow, color = Ink, maxLines = 1, softWrap = false)
                    model.chip?.let { Deadline(it) }
                }
            }
        }
        Box(
            modifier = Modifier
                // The icon, not its 48dp box, aligns with the gutter.
                .offset(x = 12.dp)
                .size(Touch.target)
                .pressClickable(onClick = onRemove)
                .semantics { contentDescription = "Remover dos favoritos: ${model.title}" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Favorite, contentDescription = null, tint = AmberText, modifier = Modifier.size(24.dp))
        }
    }
}

/** The time beside a saved price: clock and words, amber when it is the last day. */
@Composable
private fun Deadline(spec: ChipSpec) {
    val color = if (spec.hot) AmberText else Ink60
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        if (spec.clock) {
            Icon(Icons.Outlined.Schedule, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
        }
        Text(
            text = spec.text,
            style = Type.caption.sized(12.5, 16.0, FontWeight.SemiBold),
            color = color,
            maxLines = 1,
        )
    }
}
