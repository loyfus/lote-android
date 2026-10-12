package br.com.loyfus.ui.components

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.loyfus.ui.theme.Ink60
import br.com.loyfus.ui.theme.Motion
import br.com.loyfus.ui.theme.PaperDim
import br.com.loyfus.ui.theme.Type
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.decode.DataSource

private const val AMBIENT_ALPHA = 0.62f
private const val AMBIENT_SCALE = 1.3f
private val AMBIENT_BLUR = 22.dp

// The glow is the photo's own colour a little more saturated, so it reads as light
// spilling out of the picture rather than as a grey mat.
private val AmbientFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1.15f) })

/**
 * A photograph on its plate, never cropped: seller banners and watermarks are part of the
 * listing and cutting them made plates read as broken images. It sits in `Fit` on an ambient
 * layer — the same picture filling the plate, blurred and dimmed — so a portrait photo in a
 * landscape plate is framed by its own colours instead of grey bars.
 *
 * The blur is a render effect, API 31+, and below that the ambient layer is simply not drawn;
 * both layers share one painter, so the image is requested and decoded once.
 *
 * [url] may be blank: a lot with no photo is normal here. Pass a [contentDescription] only
 * where the photo is the content (the gallery); in a list the title beside it is the text.
 */
@Composable
fun PhotoPlate(
    url: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    contentDescription: String? = null,
    ambient: Boolean = true,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier.clip(shape).background(PaperDim)) {
        if (url.isNullOrBlank()) {
            NoPhoto(Modifier.matchParentSize())
        } else {
            Photo(url, contentDescription, ambient)
        }
        overlay()
    }
}

@Composable
private fun BoxScope.Photo(url: String, contentDescription: String?, ambient: Boolean) {
    val painter = rememberAsyncImagePainter(model = url, contentScale = ContentScale.Fit)
    val state by painter.state.collectAsState()

    if (state is AsyncImagePainter.State.Error) {
        NoPhoto(Modifier.matchParentSize())
        return
    }

    val loaded = state as? AsyncImagePainter.State.Success
    // A photo that comes straight from memory (a tile scrolled back into view) appears
    // at once; fading it in again would make a list flicker as it scrolls.
    val instant = loaded?.result?.dataSource == DataSource.MEMORY_CACHE
    val fade by animateFloatAsState(
        targetValue = if (loaded != null) 1f else 0f,
        animationSpec = if (instant) snap() else tween(Motion.STANDARD),
        label = "photo",
    )

    if (ambient && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            colorFilter = AmbientFilter,
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    alpha = AMBIENT_ALPHA * fade
                    clip = true
                    val radius = AMBIENT_BLUR.toPx()
                    renderEffect = BlurEffect(radius, radius, TileMode.Clamp)
                }
                // Inside the blur layer, so the 22 is measured on the enlarged picture.
                .graphicsLayer {
                    scaleX = AMBIENT_SCALE
                    scaleY = AMBIENT_SCALE
                },
        )
    }
    Image(
        painter = painter,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .matchParentSize()
            .graphicsLayer { alpha = fade },
    )
}

@Composable
private fun NoPhoto(modifier: Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = Ink60, modifier = Modifier.size(24.dp))
        Text("Sem foto", style = Type.caption.copy(fontWeight = FontWeight.SemiBold), color = Ink60)
    }
}
