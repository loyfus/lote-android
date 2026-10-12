package br.com.loyfus.ui.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import br.com.loyfus.ui.components.HeartButton
import br.com.loyfus.ui.components.PhotoIconButton
import br.com.loyfus.ui.components.PhotoPlate
import br.com.loyfus.ui.components.pressClickable
import br.com.loyfus.ui.theme.Scrim
import br.com.loyfus.ui.SystemBarIcons
import br.com.loyfus.ui.theme.Type
import br.com.loyfus.ui.theme.sized
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * The gallery's height: 80% of the width, but never more than 42% of the window's height
 * (a phone held sideways would otherwise be all photograph), kept between 200 and 360dp.
 */
@Composable
fun galleryHeight(): Dp {
    val config = LocalConfiguration.current
    return minOf(config.screenWidthDp * 0.8f, config.screenHeightDp * 0.42f).dp.coerceIn(200.dp, 360.dp)
}

/**
 * The photographs of the lot, full width, on their ambient plate and never cropped, swiped one by
 * one; a tap opens the viewer. The controls float on the photo — 42dp scrim discs inside 48dp
 * targets — and scroll away with it, the collapsed top bar taking over when they have.
 */
@Composable
fun Gallery(
    images: List<String>,
    pager: PagerState,
    favourite: Boolean,
    title: String,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onToggleFavourite: () -> Unit,
    onOpenViewer: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth().height(galleryHeight())) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            PhotoPlate(
                url = images.getOrNull(page),
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (images.isEmpty()) Modifier else Modifier.clickable(onClickLabel = "Ampliar foto") { onOpenViewer(page) },
                    ),
                contentDescription = if (images.isEmpty()) null else "Foto ${page + 1} de ${images.size}",
            )
        }

        // A light veil under the status bar: its white icons stay legible on a white photograph.
        Box(
            Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .height(96.dp)
                .background(Brush.verticalGradient(listOf(Color(0x59000000), Color.Transparent))),
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                // The 42dp disc starts at the status bar and sits 16dp from the edge: its
                // 48dp target begins 3dp earlier on both axes.
                .offset(y = (-3).dp)
                .padding(horizontal = 13.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PhotoIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", onBack)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                PhotoIconButton(Icons.Outlined.IosShare, "Compartilhar", onShare)
                HeartButton(
                    on = favourite,
                    title = title,
                    onToggle = onToggleFavourite,
                    size = 42.dp,
                    iconSize = 22.dp,
                )
            }
        }

        if (images.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = 14.dp)
                    .defaultMinSize(minHeight = 26.dp)
                    .clip(CircleShape)
                    .background(Scrim)
                    .padding(horizontal = 10.dp)
                    .semantics { contentDescription = "Foto ${pager.currentPage + 1} de ${images.size}" },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                Text(
                    text = "${pager.currentPage + 1} / ${images.size}",
                    style = Type.caption.sized(12.5, 16.0, FontWeight.SemiBold),
                    color = Color.White,
                )
            }
        }
    }
}

/**
 * The full-screen viewer: black ground, the photographs in a pager, close on the left, "3 / 34"
 * in the middle, share on the right, thumbnails along the bottom. Pinch zooms 1x–4x, a double tap
 * goes to 2.5x and back, and a zoomed photograph is dragged while the pager stands still.
 */
@Composable
fun PhotoViewer(
    images: List<String>,
    startIndex: Int,
    /** Closes the viewer, handing back the photo it ended on so the gallery can follow. */
    onClose: (Int) -> Unit,
    onShare: () -> Unit,
) {
    val pager = rememberPagerState(initialPage = startIndex.coerceIn(0, images.lastIndex)) { images.size }
    val scope = rememberCoroutineScope()
    var zoomed by remember { mutableStateOf(false) }
    // A new page always starts at 1x, however it was reached.
    LaunchedEffect(pager.currentPage) { zoomed = false }

    BackHandler { onClose(pager.currentPage) }
    SystemBarIcons(whiteStatus = true, whiteNavigation = true)

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !zoomed,
        ) { page ->
            ZoomablePhoto(
                url = images[page],
                description = "Foto ${page + 1} de ${images.size}",
                onZoomChange = { if (page == pager.currentPage) zoomed = it },
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PhotoIconButton(Icons.Filled.Close, "Fechar", { onClose(pager.currentPage) }, disc = Color.White.copy(alpha = 0.14f))
            Text(
                text = "${pager.currentPage + 1} / ${images.size}",
                style = Type.cardTitle,
                color = Color.White,
                modifier = Modifier.semantics { contentDescription = "Foto ${pager.currentPage + 1} de ${images.size}" },
            )
            PhotoIconButton(Icons.Outlined.IosShare, "Compartilhar", onShare, disc = Color.White.copy(alpha = 0.14f))
        }

        Thumbnails(
            images = images,
            current = pager.currentPage,
            onSelect = { scope.launch { pager.animateScrollToPage(it) } },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 30.dp),
        )
    }
}

/** The strip along the bottom of the viewer: 60dp squares, the current one outlined in white. */
@Composable
private fun Thumbnails(
    images: List<String>,
    current: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strip = rememberLazyListState()
    LaunchedEffect(current) { strip.animateScrollToItem(max(0, current - 2)) }
    LazyRow(
        state = strip,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(images) { index, url ->
            val selected = index == current
            AsyncImage(
                model = url,
                contentDescription = "Foto ${index + 1} de ${images.size}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(60.dp)
                    .graphicsLayer { alpha = if (selected) 1f else 0.55f }
                    .clip(RoundedCornerShape(8.dp))
                    .then(
                        if (selected) {
                            Modifier.border(2.dp, Color.White, RoundedCornerShape(8.dp))
                        } else {
                            Modifier
                        },
                    )
                    .pressClickable(onClick = { onSelect(index) }, role = Role.Button)
                    .semantics { this.selected = selected },
            )
        }
    }
}

private const val MAX_ZOOM = 4f
private const val DOUBLE_TAP_ZOOM = 2.5f

/**
 * One photograph in the viewer, with pinch, drag and double tap. A single finger at 1x is not
 * consumed, so the pager still swipes; two fingers, or any finger on a zoomed photo, are the
 * zoom's.
 */
@Composable
private fun ZoomablePhoto(
    url: String,
    description: String,
    onZoomChange: (Boolean) -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    // The farthest the photograph may be dragged: its enlarged edge reaches the screen's.
    fun clamp(raw: Offset, atScale: Float): Offset {
        val maxX = size.width * (atScale - 1f) / 2f
        val maxY = size.height * (atScale - 1f) / 2f
        return Offset(raw.x.coerceIn(-maxX, maxX), raw.y.coerceIn(-maxY, maxY))
    }

    val painter = rememberAsyncImagePainter(model = url, contentScale = ContentScale.Fit)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tap ->
                        if (scale > 1.01f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            val centre = Offset(size.width / 2f, size.height / 2f)
                            scale = DOUBLE_TAP_ZOOM
                            // Zoom around the tap: the point under the finger stays under it.
                            offset = clamp((centre - tap) * (DOUBLE_TAP_ZOOM - 1f), DOUBLE_TAP_ZOOM)
                        }
                        onZoomChange(scale > 1f)
                    },
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val fingers = event.changes.count { it.pressed }
                        if (fingers > 1 || scale > 1f) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val newScale = (scale * zoom).coerceIn(1f, MAX_ZOOM)
                            val applied = newScale / scale
                            val centroid = event.calculateCentroid(useCurrent = true)
                            val about = if (centroid.isSpecified) {
                                centroid - Offset(size.width / 2f, size.height / 2f)
                            } else {
                                Offset.Zero
                            }
                            scale = newScale
                            offset = if (newScale <= 1f) Offset.Zero else clamp(offset * applied + about * (1f - applied) + pan, newScale)
                            onZoomChange(scale > 1f)
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
    ) {
        Image(
            painter = painter,
            contentDescription = description,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
