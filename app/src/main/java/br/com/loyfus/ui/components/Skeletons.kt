package br.com.loyfus.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.loyfus.ui.theme.Adaptive
import br.com.loyfus.ui.theme.Corners
import br.com.loyfus.ui.theme.Glass
import br.com.loyfus.ui.theme.LocalReducedMotion
import br.com.loyfus.ui.theme.Motion
import br.com.loyfus.ui.theme.PaperDim
import br.com.loyfus.ui.theme.Space
import br.com.loyfus.ui.theme.Surface

// Placeholders with the final geometry, so nothing jumps when the content arrives;
// the content then fades in over them (Motion.contentSwap). They breathe rather than
// shimmer: a sweep is motion the eye follows, a slow dimming only says "not yet".

/**
 * A loading placeholder breathes: the whole group dims to 55% and back over 1.8 s — one
 * animation for the group, read in the layer so it never recomposes. Reduced motion: it stands
 * still.
 */
@Composable
private fun Modifier.pulse(): Modifier {
    if (LocalReducedMotion.current) return this
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha = transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(Motion.PULSE, easing = Motion.Standard), RepeatMode.Reverse),
        label = "pulse",
    )
    return graphicsLayer { this.alpha = alpha.value }
}

/** What a screen reader hears while a placeholder is on screen: one word, not a list of blanks. */
private const val LOADING = "Carregando"

@Composable
private fun Block(modifier: Modifier, shape: Shape = Corners.tag, color: Color = PaperDim) {
    Box(modifier.clip(shape).background(color))
}

@Composable
private fun TileSkeleton(style: TileStyle, modifier: Modifier = Modifier) {
    Column(modifier) {
        Block(Modifier.fillMaxWidth().aspectRatio(style.ratio), style.corners)
        Column(Modifier.padding(start = 2.dp, top = 10.dp, end = 2.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Block(Modifier.fillMaxWidth().height(14.dp))
            Block(Modifier.fillMaxWidth(0.6f).height(14.dp))
            Block(Modifier.padding(top = 2.dp).width(96.dp).height(18.dp))
        }
    }
}

/**
 * Home while it loads: the hero's parts, then the sheet's. Both are placeholders with the final
 * geometry, so nothing jumps when the page arrives.
 */
@Composable
fun HomeHeroSkeleton(showBanner: Boolean, modifier: Modifier = Modifier) {
    // On the night blue the placeholders are the hero's own glass, not the paper tone.
    val glass = Glass
    Column(modifier.pulse().clearAndSetSemantics { }) {
        Block(
            Modifier.padding(horizontal = Space.gutter).fillMaxWidth().height(54.dp),
            RoundedCornerShape(27.dp),
            glass,
        )
        Row(
            Modifier.padding(start = Space.gutter, top = 18.dp, end = Space.gutter),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(5) {
                Column(Modifier.width(74.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Block(Modifier.size(64.dp), RoundedCornerShape(22.dp), glass)
                    Block(Modifier.padding(top = 8.dp).width(48.dp).height(12.dp), color = glass)
                }
            }
        }
        if (showBanner) {
            Block(
                Modifier.padding(start = 16.dp, top = 16.dp, end = Space.gutter).fillMaxWidth().height(82.dp),
                RoundedCornerShape(22.dp),
                glass,
            )
        }
    }
}

/** The sheet's skeleton: the context line, the two status cards and one shelf of two tiles. */
@Composable
fun HomeSheetSkeleton(modifier: Modifier = Modifier) {
    Column(modifier.pulse().clearAndSetSemantics { contentDescription = LOADING }) {
        Block(Modifier.padding(start = Space.gutter, bottom = 14.dp).width(180.dp).height(14.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Space.gutter),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            repeat(2) { Block(Modifier.weight(1f).height(132.dp), RoundedCornerShape(22.dp)) }
        }
        Block(
            Modifier.padding(start = Space.gutter, top = 12.dp, end = Space.gutter)
                .fillMaxWidth().height(140.dp),
            RoundedCornerShape(22.dp),
        )
        Block(Modifier.padding(start = Space.gutter, top = 28.dp).width(190.dp).height(24.dp))
        Block(Modifier.padding(start = Space.gutter, top = 8.dp).width(240.dp).height(14.dp))
        Row(
            Modifier.padding(start = Space.gutter, top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            repeat(2) { TileSkeleton(TileStyle.Rail, Modifier.width(RailTileWidth)) }
        }
    }
}

/** Explore while a page loads: [rows] rows of tiles, at the column count the grid will have. */
@Composable
fun GridSkeleton(columns: Int, rows: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(horizontal = Space.gutter)
            .pulse()
            .clearAndSetSemantics { contentDescription = LOADING },
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        repeat(rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(columns) { TileSkeleton(TileStyle.Grid, Modifier.weight(1f)) }
            }
        }
    }
}

/** Saved while it loads: a title and a few rows. */
@Composable
fun SavedSkeleton(modifier: Modifier = Modifier) {
    Column(modifier.pulse().clearAndSetSemantics { contentDescription = LOADING }) {
        Block(Modifier.padding(start = Space.gutter, top = 6.dp).width(120.dp).height(34.dp))
        Block(Modifier.padding(start = Space.gutter, top = 8.dp).width(180.dp).height(14.dp))
        Block(Modifier.padding(start = Space.gutter, top = 28.dp).width(140.dp).height(14.dp))
        repeat(3) {
            Row(
                Modifier.padding(horizontal = Space.gutter, vertical = Space.md),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Block(Modifier.size(width = 104.dp, height = 78.dp), Corners.control)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Block(Modifier.fillMaxWidth().height(14.dp))
                    Block(Modifier.fillMaxWidth(0.6f).height(14.dp))
                    Block(Modifier.padding(top = 2.dp).width(96.dp).height(18.dp))
                }
            }
        }
    }
}

/** The detail while it loads: the gallery plate, the title, and the decision card. */
@Composable
fun DetailSkeleton(galleryHeight: Dp, modifier: Modifier = Modifier) {
    Column(modifier.pulse().clearAndSetSemantics { contentDescription = LOADING }) {
        Block(Modifier.fillMaxWidth().height(galleryHeight), androidx.compose.ui.graphics.RectangleShape)
        Column(
            Modifier
                .widthIn(max = Adaptive.maxContentWidth)
                .fillMaxWidth()
                .padding(start = Space.gutter, top = Space.lg, end = Space.gutter),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Block(Modifier.width(96.dp).height(28.dp))
                Block(Modifier.width(64.dp).height(28.dp))
            }
            Block(Modifier.padding(top = 12.dp).fillMaxWidth().height(24.dp))
            Block(Modifier.padding(top = 8.dp).fillMaxWidth(0.7f).height(24.dp))
            Box(
                Modifier
                    .padding(top = 18.dp)
                    .fillMaxWidth()
                    .height(176.dp)
                    .clip(Corners.decision)
                    .background(Surface),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Block(Modifier.width(84.dp).height(14.dp))
                    Block(Modifier.width(190.dp).height(34.dp))
                    Block(Modifier.width(150.dp).height(14.dp))
                    Block(Modifier.padding(top = 14.dp).width(160.dp).height(20.dp))
                }
            }
        }
    }
}
