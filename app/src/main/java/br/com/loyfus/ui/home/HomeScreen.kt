package br.com.loyfus.ui.home

import androidx.compose.foundation.background
import br.com.loyfus.ui.theme.Motion
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.loyfus.core.LotKind
import br.com.loyfus.core.Urgency
import br.com.loyfus.data.ShelfQuery
import br.com.loyfus.ui.SystemBarIcons
import br.com.loyfus.ui.components.ErrorState
import br.com.loyfus.ui.components.HomeHeroSkeleton
import br.com.loyfus.ui.components.HomeSheetSkeleton
import br.com.loyfus.ui.components.LotCardModel
import br.com.loyfus.ui.components.QuietButton
import br.com.loyfus.ui.components.RailTile
import br.com.loyfus.ui.components.pressClickable
import br.com.loyfus.ui.theme.Corners
import br.com.loyfus.ui.theme.HeroTop
import br.com.loyfus.ui.theme.Ink
import br.com.loyfus.ui.theme.Ink60
import br.com.loyfus.ui.theme.Paper
import br.com.loyfus.ui.theme.Space
import br.com.loyfus.ui.theme.Type
import br.com.loyfus.ui.theme.sized
import kotlinx.coroutines.launch

/**
 * Home is the door of the catalogue: a hero that says who this is and hands over to search in one
 * tap, the categories as frosted shortcuts, one notice it can take back, and a sheet that says
 * what is true right now. It is not a feed — browsing is Explore's job, and Home's job is the first
 * screen of it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    favouriteIds: Set<Long>,
    savedLots: List<LotCardModel>,
    bannerDismissed: Boolean,
    onDismissBanner: () -> Unit,
    onSetFavourite: (Long, Boolean) -> Unit,
    onOpenLot: (Long) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenShelf: (ShelfQuery) -> Unit,
    onOpenCategory: (String) -> Unit,
    onOpenAllCategories: () -> Unit,
    onOpenBudget: (Long?, Long?) -> Unit,
    onOpenSaved: () -> Unit,
    onOpenTab: (HomeTab) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAbout by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // White status icons on the night-blue bar, in both themes.
    SystemBarIcons(whiteStatus = true)

    HomeMenu(
        drawerState = drawerState,
        categories = state.categories,
        currentTab = HomeTab.Home,
        savedCount = favouriteIds.size,
        scope = scope,
        onSelectTab = onOpenTab,
        onSelectCategory = { id ->
            onOpenCategory(id)
        },
    ) {
        val heroTop = HeroTop
        val paper = Paper
        Column(Modifier.fillMaxSize()) {
            // The bar does not scroll. It is the hero's own night blue, so the status bar
            // always sits on the brand's ground, and its white icons stay true however
            // far down the list goes.
            HomeBar(
                onOpenMenu = { scope.launch { drawerState.open() } },
                onOpenSettings = onOpenSettings,
                modifier = Modifier
                    .background(heroTop)
                    .windowInsetsPadding(WindowInsets.statusBars),
            )
            PullToRefreshBox(
                isRefreshing = state.pulling,
                onRefresh = { viewModel.load(manual = true) },
                modifier = Modifier
                    .fillMaxSize()
                    // Night blue above the middle, paper below: whatever an overscroll
                    // reveals continues the edge it was pulled from.
                    .drawBehind {
                        drawRect(heroTop, size = Size(size.width, size.height / 2))
                        drawRect(paper, topLeft = Offset(0f, size.height / 2), size = Size(size.width, size.height / 2))
                    },
            ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // The sheet is drawn 28dp over the hero; the extra bottom padding is
                // what pays for the overlap, so the last row can still be scrolled to.
                contentPadding = PaddingValues(bottom = Space.x3 + 28.dp),
            ) {
                item(key = "hero") {
                    // The placeholder gives way to the hero by fading, never by a cut.
                    AnimatedContent(
                        targetState = state.loaded || state.error != null,
                        transitionSpec = { Motion.contentSwap },
                        label = "hero",
                    ) { ready ->
                        if (ready) {
                            HomeHero(
                                categories = state.categories,
                                showBanner = !bannerDismissed,
                                onOpenSearch = onOpenSearch,
                                onOpenCategory = onOpenCategory,
                                onOpenAllCategories = onOpenAllCategories,
                                onOpenAbout = { showAbout = true },
                                onDismissBanner = onDismissBanner,
                            )
                        } else {
                            HomeHeroSkeleton(
                                showBanner = !bannerDismissed,
                                Modifier
                                    .background(heroBrush())
                                    .padding(top = Space.xs, bottom = 46.dp),
                            )
                        }
                    }
                }
                item(key = "sheet") {
                    Column(
                        Modifier
                            .offset(y = (-28).dp)
                            .fillMaxWidth()
                            .clip(Corners.sheet)
                            .background(Paper)
                            .padding(top = 22.dp),
                    ) {
                        val phase = when {
                            state.loaded -> SheetPhase.Ready
                            state.error != null -> SheetPhase.Failed
                            else -> SheetPhase.Loading
                        }
                        // Keyed by the phase, not by the data: a refresh that brings new
                        // shelves updates them in place instead of fading the sheet again.
                        AnimatedContent(targetState = phase, transitionSpec = { Motion.contentSwap }, label = "sheet") { shown ->
                            when (shown) {
                                SheetPhase.Failed -> ErrorState(
                                    message = state.error.orEmpty(),
                                    onRetry = { viewModel.load(manual = false) },
                                    modifier = Modifier.fillMaxWidth().padding(bottom = Space.x3),
                                )
                                SheetPhase.Loading -> HomeSheetSkeleton(
                                    modifier = Modifier.padding(bottom = Space.x3),
                                )
                                SheetPhase.Ready -> Sheet(
                                    state = state,
                                    savedLots = savedLots,
                                    favouriteIds = favouriteIds,
                                    onSetFavourite = onSetFavourite,
                                    onOpenLot = onOpenLot,
                                    onOpenShelf = onOpenShelf,
                                    onOpenBudget = onOpenBudget,
                                    onOpenSaved = onOpenSaved,
                                )
                            }
                        }
                    }
                }
            }
            }
        }
    }

    if (showAbout) {
        AboutSheet(
            totalLots = state.totalLots,
            updatedAt = state.updatedAt,
            onDismiss = { showAbout = false },
        )
    }
}

/** Everything under the hero: what is true now, what to spend, and what to look at. */
@Composable
private fun Sheet(
    state: HomeUiState,
    savedLots: List<LotCardModel>,
    favouriteIds: Set<Long>,
    onSetFavourite: (Long, Boolean) -> Unit,
    onOpenLot: (Long) -> Unit,
    onOpenShelf: (ShelfQuery) -> Unit,
    onOpenBudget: (Long?, Long?) -> Unit,
    onOpenSaved: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        ContextLine(updatedAt = state.updatedAt, totalLots = state.totalLots)
        StatusCards(
            closing = state.closing,
            acceptsProposal = state.acceptsProposal,
            onOpenClosing = { window ->
                onOpenShelf(ShelfQuery(sort = "ending", modality = "LEILAO", endsWithin = window.withinHours))
            },
            onOpenOffers = { onOpenShelf(ShelfQuery(sort = "ending", acceptsProposal = true)) },
        )
        BudgetCard(onPick = onOpenBudget, modifier = Modifier.padding(bottom = 12.dp))

        val saved = savedLots.forHome()
        if (saved.isNotEmpty()) {
            Shelf(title = "Seus salvos", subtitle = null, onOpenAll = onOpenSaved) {
                items(saved, key = { "saved-" + it.id }) { model ->
                    RailTile(
                        model = model,
                        favourite = true,
                        onOpen = { onOpenLot(model.id) },
                        onToggleFavourite = { onSetFavourite(model.id, false) },
                    )
                }
            }
        }

        state.shelves.forEach { shelf ->
            Shelf(
                title = shelf.title,
                subtitle = shelf.subtitle,
                onOpenAll = { onOpenShelf(shelf.query) },
            ) {
                items(shelf.items, key = { shelf.id + "-" + it.id }) { model ->
                    RailTile(
                        model = model,
                        favourite = model.id in favouriteIds,
                        onOpen = { onOpenLot(model.id) },
                        onToggleFavourite = { onSetFavourite(model.id, model.id !in favouriteIds) },
                    )
                }
            }
        }
    }
}

/** A shelf: its title, the line that says what it is, and the rail of tiles. */
@Composable
private fun Shelf(
    title: String,
    subtitle: String?,
    onOpenAll: () -> Unit,
    tiles: LazyListScope.() -> Unit,
) {
    Column(Modifier.padding(top = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = Space.gutter, end = Space.gutter),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = Type.headline,
                color = Ink,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            QuietButton(label = "Ver todos", onClick = onOpenAll, icon = Icons.Outlined.ChevronRight)
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = Type.small.sized(13.5, 18.0),
                color = Ink60,
                modifier = Modifier.padding(start = Space.gutter, top = 2.dp, end = Space.gutter),
            )
        }
        val railState = rememberLazyListState()
        LazyRow(
            state = railState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = Space.gutter, top = 14.dp, end = Space.gutter),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            flingBehavior = rememberSnapFlingBehavior(railState),
            content = tiles,
        )
    }
}

/**
 * The saved lots as Home shows them: closing soonest first, direct sales and lots without a
 * deadline last, and finished ones left out entirely — a rail of lots whose time has passed is a
 * shelf of nothing.
 */
private fun List<LotCardModel>.forHome(): List<LotCardModel> = filter { it.urgency != Urgency.Ended }
    .sortedWith(
        compareBy(
            { it.urgency == Urgency.None || it.kind == LotKind.DirectSale },
            { it.secondsToClose },
        ),
    )
    .take(10)

/**
 * The notice's answers: what the app is, where the data comes from, what it keeps. Opened
 * by the notice on Home and by "Como funciona" in Settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AboutSheet(
    totalLots: Long,
    updatedAt: String?,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = Corners.sheet,
        containerColor = Paper,
        contentColor = Ink,
        scrimColor = Color.Black.copy(alpha = 0.45f),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = Space.gutter)
                .padding(bottom = Space.xl),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Sobre o Loyfus",
                    style = Type.headline,
                    color = Ink,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                Box(
                    modifier = Modifier.size(48.dp).pressClickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Fechar", tint = Ink60, modifier = Modifier.size(24.dp))
                }
            }
            AboutSections(totalLots = totalLots, updatedAt = updatedAt)
        }
    }
}

/** Which of its three faces the sheet shows; the cross-fade runs only when this changes. */
private enum class SheetPhase { Loading, Failed, Ready }
