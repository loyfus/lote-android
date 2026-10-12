package br.com.loyfus.ui.detail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.loyfus.ui.SystemBarIcons
import br.com.loyfus.ui.components.DetailSkeleton
import br.com.loyfus.ui.components.EmptyState
import br.com.loyfus.ui.components.ErrorState
import br.com.loyfus.ui.components.PhotoIconButton
import br.com.loyfus.ui.components.SecondaryButton
import br.com.loyfus.ui.components.pressClickable
import br.com.loyfus.ui.dial
import br.com.loyfus.ui.openUrl
import br.com.loyfus.ui.share
import br.com.loyfus.ui.theme.Adaptive
import br.com.loyfus.ui.theme.AmberText
import br.com.loyfus.ui.theme.Ink
import br.com.loyfus.ui.theme.Ink60
import br.com.loyfus.ui.theme.Line
import br.com.loyfus.ui.theme.LocalReducedMotion
import br.com.loyfus.ui.theme.Motion
import br.com.loyfus.ui.theme.Paper
import br.com.loyfus.ui.theme.Space
import br.com.loyfus.ui.theme.Touch
import br.com.loyfus.ui.theme.Type
import br.com.loyfus.ui.writeTo
import kotlin.math.max
import kotlinx.coroutines.launch

/**
 * A lot. The photograph first, then what it is, what it costs and for how long, then the
 * details, then the auction behind it; a bar fixed to the bottom carries the price and the
 * way to the source from the first pixel to the last. The navigation bar is not on this
 * screen: the controls float on the photograph, and give way to a plain top bar once the
 * photograph has scrolled off.
 */
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    favouriteIds: Set<Long>,
    favouritesLoaded: Boolean,
    onSetFavourite: (Long, Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val model = state.model

    val phase = when {
        state.loading -> LotPhase.Loading
        model == null -> LotPhase.Failed
        else -> LotPhase.Ready
    }
    // The skeleton has the lot's geometry, so the lot fades in over it without anything
    // moving. Keyed by the phase: a favourite or a fresh read does not fade the screen.
    AnimatedContent(
        targetState = phase,
        modifier = Modifier.fillMaxSize().background(Paper),
        transitionSpec = { Motion.contentSwap },
        label = "lot",
    ) { shown ->
        when (shown) {
            LotPhase.Loading -> Loading(onBack)
            LotPhase.Failed -> Failed(notFound = state.notFound, message = state.error.orEmpty(), onRetry = viewModel::load, onBack = onBack)
            LotPhase.Ready -> if (model != null) {
                // Until the device's list of saved lots has arrived, the lot's own flag answers.
                val saved = if (favouritesLoaded) model.id in favouriteIds else model.lot.isFavourite
                Loaded(
                    model = model,
                    favourite = saved,
                    onToggleFavourite = { onSetFavourite(model.id, !saved) },
                    onBack = onBack,
                )
            }
        }
    }
}

@Composable
private fun Loading(onBack: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        DetailSkeleton(galleryHeight = galleryHeight())
        PhotoIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            label = "Voltar",
            onClick = onBack,
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(start = 13.dp),
        )
    }
}

@Composable
private fun Failed(notFound: Boolean, message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
        BarButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            label = "Voltar",
            tint = Ink,
            onClick = onBack,
            modifier = Modifier.padding(start = 8.dp),
        )
        if (notFound) {
            EmptyState(
                icon = Icons.Outlined.SearchOff,
                title = "Lote não encontrado",
                message = "Este lote pode ter sido removido da fonte desde a última atualização.",
                modifier = Modifier.weight(1f),
                action = { SecondaryButton(label = "Voltar", onClick = onBack) },
            )
        } else {
            ErrorState(message = message, onRetry = onRetry, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun Loaded(
    model: DetailModel,
    favourite: Boolean,
    onToggleFavourite: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val lot = model.lot
    val images = lot.images

    val listState = rememberLazyListState()
    val pager = rememberPagerState { max(images.size, 1) }
    var viewerAt by rememberSaveable { mutableIntStateOf(-1) }
    var paused by rememberSaveable { mutableStateOf(false) }
    val seconds = rememberLiveSeconds(lot.secondsToClose, model.readAtElapsed, paused)

    // The top bar appears once the photograph has scrolled up under where it would sit.
    val galleryPx = with(density) { galleryHeight().toPx() }
    val barPx = with(density) { TOP_BAR_HEIGHT.toPx() } + WindowInsets.statusBars.getTop(density)
    val collapsed by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > galleryPx - barPx
        }
    }
    // White icons over the photograph, the theme's over the paper bar.
    SystemBarIcons(whiteStatus = !collapsed)

    val share = { context.share(model.shareText) }
    val readable = Modifier
        .widthIn(max = Adaptive.maxContentWidth)
        .fillMaxWidth()
        .padding(horizontal = Space.gutter)

    Box(Modifier.fillMaxSize()) {
        // The list ends where the bar begins: nothing scrolls beneath the bar, so it is
        // paper over paper and never shows a ghost of the text it would hide.
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = Space.xxl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    item(key = "gallery") {
                        Gallery(
                            images = images,
                            pager = pager,
                            favourite = favourite,
                            title = model.title,
                            onBack = onBack,
                            onShare = share,
                            onToggleFavourite = onToggleFavourite,
                            onOpenViewer = { viewerAt = it },
                        )
                    }
                    item(key = "intro") {
                        Column(readable.padding(top = Space.lg)) {
                            Kicker(model, Modifier.padding(bottom = Space.md))
                            Text(
                                text = model.title,
                                style = if (model.longTitle) Type.titleLong else Type.title,
                                color = Ink,
                                modifier = Modifier.semantics { heading() },
                            )
                            model.titleDetail?.let {
                                Text(
                                    text = it,
                                    style = Type.small,
                                    color = Ink60,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                            }
                            DecisionCard(
                                model = model,
                                seconds = seconds,
                                paused = paused,
                                onTogglePause = { paused = !paused },
                                modifier = Modifier.padding(top = 18.dp),
                            )
                            SourceNote(model, Modifier.padding(top = 14.dp))
                        }
                    }
                    if (model.summary.isNotEmpty()) {
                        item(key = "summary") { Summary(model.summary, readable.padding(top = SECTION_GAP)) }
                    }
                    lot.description?.takeIf { it.isNotBlank() }?.let { text ->
                        item(key = "description") { Description(text, readable.padding(top = SECTION_GAP)) }
                    }
                    if (model.specs.isNotEmpty()) {
                        item(key = "specs") { Specs(model.specs, readable.padding(top = SECTION_GAP)) }
                    }
                    if (model.conditions.isNotEmpty()) {
                        item(key = "conditions") { Conditions(model.conditions, readable.padding(top = SECTION_GAP)) }
                    }
                    if (lot.auction != null) {
                        item(key = "auction") {
                            AuctionCard(
                                auction = lot.auction,
                                city = lot.city,
                                state = lot.stateCode,
                                onOpenUrl = context::openUrl,
                                onMail = context::writeTo,
                                onDial = context::dial,
                                modifier = readable.padding(top = SECTION_GAP),
                            )
                        }
                    }
                }

                CollapsedBar(
                    visible = collapsed,
                    title = model.title,
                    favourite = favourite,
                    onBack = onBack,
                    onToggleFavourite = onToggleFavourite,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }

            StickyBar(model = model, seconds = seconds, onOpenSource = context::openUrl)
        }

        // The viewer opens over the lot by fading in from 96%, as if the photo came
        // forward, and closes by fading; with reduced motion it only fades.
        val reducedMotion = LocalReducedMotion.current
        AnimatedVisibility(
            visible = viewerAt >= 0 && images.isNotEmpty(),
            enter = if (reducedMotion) fadeIn(Motion.enter()) else fadeIn(Motion.enter()) + scaleIn(Motion.enter(), initialScale = 0.96f),
            exit = fadeOut(Motion.exit(Motion.STANDARD)),
        ) {
            PhotoViewer(
                images = images,
                // While it fades out the index is already -1; the pager keeps the page it is on.
                startIndex = viewerAt.coerceAtLeast(0),
                onClose = { last ->
                    scope.launch { pager.scrollToPage(last) }
                    viewerAt = -1
                },
                onShare = share,
            )
        }
    }
}

/**
 * The bar that takes over from the floating controls once the photograph has scrolled
 * off: back, the title in one line, the heart; a hairline underneath. It fades in and out.
 */
@Composable
private fun CollapsedBar(
    visible: Boolean,
    title: String,
    favourite: Boolean,
    onBack: () -> Unit,
    onToggleFavourite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(TOP_BAR_FADE)),
        exit = fadeOut(tween(TOP_BAR_FADE)),
    ) {
        CollapsedBarContent(title, favourite, onBack, onToggleFavourite)
    }
}

@Composable
private fun CollapsedBarContent(
    title: String,
    favourite: Boolean,
    onBack: () -> Unit,
    onToggleFavourite: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(Paper)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(TOP_BAR_HEIGHT)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BarButton(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", Ink, onBack)
            Text(
                text = title,
                style = Type.cardTitle,
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
            )
            BarButton(
                icon = if (favourite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                label = if (favourite) "Remover dos favoritos: $title" else "Favoritar $title",
                tint = if (favourite) AmberText else Ink,
                onClick = onToggleFavourite,
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
    }
}

/** A plain icon button on the paper bar: 48dp, no disc. */
@Composable
private fun BarButton(
    icon: ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(Touch.target)
            .pressClickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
    }
}

private val TOP_BAR_HEIGHT: Dp = 48.dp
private val SECTION_GAP: Dp = 26.dp
private const val TOP_BAR_FADE = 150

/** Which of its three faces the lot shows; the cross-fade runs only when this changes. */
private enum class LotPhase { Loading, Failed, Ready }
