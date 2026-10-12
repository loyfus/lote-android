package br.com.loyfus.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.loyfus.core.PRICE_ON_REQUEST
import br.com.loyfus.ui.theme.Corners
import br.com.loyfus.ui.theme.Ink
import br.com.loyfus.ui.theme.Ink60
import br.com.loyfus.ui.theme.Type
import br.com.loyfus.ui.theme.AmberText
import br.com.loyfus.ui.theme.sized

/** The two places a tile lives: a rail on Home, a grid in Explore. */
enum class TileStyle(val ratio: Float, val corners: androidx.compose.foundation.shape.RoundedCornerShape) {
    Rail(3f / 2f, Corners.card),
    Grid(4f / 3f, Corners.control),
}

/** Width of a tile in a rail. */
val RailTileWidth: Dp = 256.dp

/**
 * The lot as the catalogue shows it: photo first, then what it is, where, what it costs. No
 * frame, no fill, no category, no status — the photo and the space around it separate one lot
 * from the next, and only the exceptional gets a badge.
 *
 * The photo carries the deadline badge and the heart. The whole tile is one button with one
 * spoken label; the heart is a stop beside it, because a toggle nested in a button cannot be
 * reached on its own.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LotTile(
    model: LotCardModel,
    favourite: Boolean,
    onOpen: () -> Unit,
    onToggleFavourite: () -> Unit,
    modifier: Modifier = Modifier,
    style: TileStyle = TileStyle.Grid,
) {
    Box(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .pressClickable(onClick = onOpen, press = Press.Surface)
                .clearAndSetSemantics { contentDescription = model.label },
        ) {
            PhotoPlate(
                url = model.lot.primaryImageUrl,
                modifier = Modifier.fillMaxWidth().aspectRatio(style.ratio),
                shape = style.corners,
            ) {
                model.chip?.let {
                    TimeChip(it, Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = 10.dp))
                }
            }
            Column(Modifier.padding(start = 2.dp, top = 10.dp, end = 2.dp)) {
                Text(
                    text = model.title,
                    style = Type.cardTitle,
                    color = Ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                model.place?.let {
                    Text(
                        text = it,
                        style = Type.small.sized(13.0, 18.0),
                        color = Ink60,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                // The caption drops under the price when the two do not fit side by
                // side; the price itself never wraps or shrinks.
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    itemVerticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text = model.price,
                        style = Type.priceTile,
                        color = when {
                            model.hot -> AmberText
                            model.price == PRICE_ON_REQUEST -> Ink60
                            else -> Ink
                        },
                        maxLines = 1,
                        softWrap = false,
                    )
                    model.caption?.let {
                        Text(
                            text = it,
                            style = Type.small.sized(13.0, 18.0),
                            color = Ink60,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }
        HeartButton(
            on = favourite,
            title = model.title,
            onToggle = onToggleFavourite,
            // The 36dp disc sits 8dp in from the corner of the photo: the 48dp target
            // around it starts 6dp closer.
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 2.dp),
        )
    }
}

/** A rail tile: the same tile at the rail's fixed width. */
@Composable
fun RailTile(
    model: LotCardModel,
    favourite: Boolean,
    onOpen: () -> Unit,
    onToggleFavourite: () -> Unit,
) = LotTile(
    model = model,
    favourite = favourite,
    onOpen = onOpen,
    onToggleFavourite = onToggleFavourite,
    modifier = Modifier.width(RailTileWidth),
    style = TileStyle.Rail,
)
