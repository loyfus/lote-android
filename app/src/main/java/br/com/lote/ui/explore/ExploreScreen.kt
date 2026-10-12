package br.com.lote.ui.explore

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.lote.core.categoryShortLabel
import br.com.lote.core.groupedCount
import br.com.lote.data.Category
import br.com.lote.data.LotSort
import br.com.lote.ui.components.AnimatedBadge
import br.com.lote.ui.components.AnimatedCount
import br.com.lote.ui.components.Chip
import br.com.lote.ui.components.EmptyState
import br.com.lote.ui.components.ErrorState
import br.com.lote.ui.components.GridSkeleton
import br.com.lote.ui.components.LotCardModel
import br.com.lote.ui.components.LotTile
import br.com.lote.ui.components.Press
import br.com.lote.ui.components.QuietButton
import br.com.lote.ui.components.RemovableTag
import br.com.lote.ui.components.SearchField
import br.com.lote.ui.components.SecondaryButton
import br.com.lote.ui.components.categoryIcon
import br.com.lote.ui.components.pressClickable
import br.com.lote.ui.components.touchOverflow
import br.com.lote.ui.theme.Adaptive
import br.com.lote.ui.theme.Amber
import br.com.lote.ui.theme.Corners
import br.com.lote.ui.theme.Ink
import br.com.lote.ui.theme.Ink60
import br.com.lote.ui.theme.LineControl
import br.com.lote.ui.theme.Motion
import br.com.lote.ui.theme.OnAmber
import br.com.lote.ui.theme.Paper
import br.com.lote.ui.theme.Space
import br.com.lote.ui.theme.Surface
import br.com.lote.ui.theme.Type

/**
 * Explore: search, filter, sort and browse the catalogue. Only the search row stays put; chips,
 * active filters, the count and the grid scroll as one, so a phone held sideways is not left with
 * a quarter of a screen of results.
 *
 * The grid is rows of tiles inside one lazy column, letting chip rows run to the screen edge
 * while tiles keep the gutter. State lives above the navigation graph, so leaving and coming back
 * finds the same results in the same place.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExploreScreen(
    viewModel: ExploreViewModel,
    favouriteIds: Set<Long>,
    onSetFavourite: (Long, Boolean) -> Unit,
    onOpenLot: (Long) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var searchFocused by remember { mutableStateOf(false) }
    var showFilters by rememberSaveable { mutableStateOf(false) }

    val columns = Adaptive.gridColumns(LocalConfiguration.current.screenWidthDp.toFloat(), LocalDensity.current.fontScale)

    // Another screen asked for the keyboard (Home's search button). Counted, not
    // flagged, so a request made twice in a row is answered twice; a value restored
    // from an older run never counts as a request.
    var answered by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(state.focusRequest) {
        if (state.focusRequest > answered) {
            answered = state.focusRequest
            runCatching { focusRequester.requestFocus() }
            keyboard?.show()
        }
    }

    // A new question starts at the top of its answers. The token seen last is saved with
    // the list state, so coming back to the same results (from a lot, from another tab)
    // keeps the place, while arriving with another question (a shelf of Home) starts over.
    var seenReset by rememberSaveable { mutableIntStateOf(state.resetToken) }
    LaunchedEffect(state.resetToken) {
        if (state.resetToken != seenReset) {
            seenReset = state.resetToken
            listState.scrollToItem(0)
        }
    }

    // Closing the keyboard (back, or the keyboard's own key) leaves the field; without
    // this the recent searches would stay up over the results with nothing to type with.
    val imeVisible = WindowInsets.isImeVisible
    var imeWasVisible by remember { mutableStateOf(false) }
    LaunchedEffect(imeVisible) {
        if (imeWasVisible && !imeVisible) focusManager.clearFocus()
        imeWasVisible = imeVisible
    }
    // Scrolling the results is reading them: the keyboard has nothing more to do.
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress && searchFocused) focusManager.clearFocus()
    }

    val showRecents = searchFocused && state.query.isEmpty()

    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= info.totalItemsCount - 3
        }
    }
    LaunchedEffect(nearEnd, state.hasNext, state.items.size) {
        if (nearEnd && state.hasNext) viewModel.loadMore()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Paper)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = Space.gutter, top = 6.dp, end = Space.gutter),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            SearchField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = state.catalogueTotal?.let { "Buscar em ${groupedCount(it)} lotes" }
                    ?: "Buscar carro, imóvel, máquina…",
                onSearch = {
                    viewModel.submit()
                    focusManager.clearFocus()
                },
                onClear = viewModel::clearSearch,
                modifier = Modifier.weight(1f),
                focusRequester = focusRequester,
                onFocusChanged = { searchFocused = it },
            )
            FilterButton(activeCount = state.filters.activeCount, onClick = { showFilters = true })
        }

        // Focusing the empty field swaps the results for the recent searches in 120 ms;
        // the results keep their scroll position (listState lives above) for the way back.
        AnimatedContent(
            targetState = showRecents,
            modifier = Modifier.weight(1f),
            transitionSpec = { fadeIn(Motion.fast()) togetherWith fadeOut(Motion.fast()) },
            label = "recents",
        ) { recentsShown ->
            if (recentsShown) {
                RecentsPanel(
                    recents = state.recents,
                    categories = state.categories,
                    onUseRecent = {
                        viewModel.useRecent(it)
                        focusManager.clearFocus()
                    },
                    onClearRecents = viewModel::clearRecents,
                    onPickCategory = {
                        viewModel.selectCategory(it)
                        focusManager.clearFocus()
                    },
                )
            } else {
                PullToRefreshBox(
                    isRefreshing = state.pulling,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = Space.xxl),
                    ) {
                        item(key = "categories", contentType = "chips") {
                            CategoryChips(
                                categories = state.categories,
                                selected = state.category,
                                onSelect = viewModel::selectCategory,
                                modifier = Modifier.padding(top = 14.dp),
                            )
                        }
                        val tags = state.filters.tags()
                        if (tags.isNotEmpty()) {
                            item(key = "tags", contentType = "tags") {
                                FlowRow(
                                    modifier = arrival()
                                        .fillMaxWidth()
                                        // A tag removed closes its gap in the row instead of snapping it shut.
                                        .animateContentSize(Motion.enter())
                                        .padding(start = Space.gutter, top = 12.dp, end = Space.gutter),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    tags.forEach { tag ->
                                        RemovableTag(label = tag.label, onRemove = { viewModel.removeFilter(tag.kind) })
                                    }
                                }
                            }
                        }
                        item(key = "header", contentType = "header") {
                            ResultHeader(
                                total = state.total,
                                sort = state.sort,
                                onSort = viewModel::selectSort,
                            )
                        }
                        when {
                            state.loading && state.items.isEmpty() -> item(key = "skeleton") {
                                GridSkeleton(columns = columns, rows = 3, modifier = arrival())
                            }
                            state.error != null && state.items.isEmpty() -> item(key = "error") {
                                ErrorState(message = state.error.orEmpty(), onRetry = viewModel::retry, modifier = arrival())
                            }
                            state.items.isEmpty() -> item(key = "empty") {
                                EmptyState(
                                    modifier = arrival(),
                                    icon = Icons.Outlined.SearchOff,
                                    title = "Nenhum lote encontrado",
                                    message = emptyMessage(state),
                                    action = {
                                        SecondaryButton(label = "Limpar filtros", onClick = viewModel::clearAll)
                                    },
                                )
                            }
                            else -> {
                                val rows = state.items.chunked(columns)
                                items(rows, key = { it.first().id }, contentType = { "tiles" }) { row ->
                                    TileRow(
                                        modifier = arrival(),
                                        models = row,
                                        columns = columns,
                                        favouriteIds = favouriteIds,
                                        onSetFavourite = onSetFavourite,
                                        onOpen = {
                                            viewModel.rememberCurrentSearch()
                                            onOpenLot(it)
                                        },
                                    )
                                }
                                if (state.loadingMore) {
                                    item(key = "more") { GridSkeleton(columns = columns, rows = 1, modifier = arrival()) }
                                }
                                if (state.moreFailed) {
                                    item(key = "retry-more") {
                                        Row(
                                            modifier = arrival().fillMaxWidth().padding(horizontal = Space.gutter),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Text(
                                                text = "Não foi possível carregar mais lotes",
                                                style = Type.small,
                                                color = Ink60,
                                                modifier = Modifier.weight(1f),
                                            )
                                            QuietButton(label = "Tentar novamente", onClick = viewModel::retryMore)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilters) {
        FilterSheet(
            initialFilters = state.filters,
            initialSort = state.sort,
            countFor = viewModel::countFor,
            onApply = { filters, sort ->
                viewModel.applyFilters(filters, sort)
                showFilters = false
            },
            onDismiss = { showFilters = false },
        )
    }
}

private fun emptyMessage(state: ExploreUiState): String =
    if (state.committed.isNotBlank()) {
        "Nada para “${state.committed}” com esses filtros. Tente outros termos ou limpe os filtros."
    } else {
        "Nenhum lote combina com esses filtros. Afrouxe um deles ou volte ao padrão."
    }

@Composable
private fun TileRow(
    models: List<LotCardModel>,
    columns: Int,
    favouriteIds: Set<Long>,
    onSetFavourite: (Long, Boolean) -> Unit,
    onOpen: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = Space.gutter, end = Space.gutter, bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        models.forEach { model ->
            val saved = model.id in favouriteIds
            LotTile(
                model = model,
                favourite = saved,
                onOpen = { onOpen(model.id) },
                onToggleFavourite = { onSetFavourite(model.id, !saved) },
                modifier = Modifier.weight(1f),
            )
        }
        // The last row keeps the column width of the others.
        repeat(columns - models.size) { Spacer(Modifier.weight(1f)) }
    }
}

/** The filter button: 52dp square like the field beside it, with the number of active filters on its corner. */
@Composable
private fun FilterButton(activeCount: Int, onClick: () -> Unit) {
    val label = if (activeCount > 0) "Filtros, $activeCount ativos" else "Filtros"
    Box(
        modifier = Modifier
            .size(52.dp)
            .pressClickable(onClick = onClick)
            .semantics { contentDescription = label },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(Corners.field)
                .background(Surface)
                .border(1.dp, LineControl, Corners.field),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Tune, contentDescription = null, tint = Ink, modifier = Modifier.size(24.dp))
        }
        AnimatedBadge(
            count = activeCount,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-6).dp),
        ) { count ->
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                    .clip(CircleShape)
                    .background(Amber)
                    .padding(horizontal = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = count.toString(), style = Type.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = OnAmber)
            }
        }
    }
}

@Composable
private fun CategoryChips(
    categories: List<Category>,
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.selectableGroup(),
        contentPadding = PaddingValues(horizontal = Space.gutter),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "all") { Chip(label = "Todas", selected = selected == null, onClick = { onSelect(null) }) }
        items(categories, key = { it.externalId }) { category ->
            Chip(
                label = categoryShortLabel(category.name),
                selected = selected == category.externalId,
                onClick = { onSelect(category.externalId) },
                icon = categoryIcon(category.name),
            )
        }
    }
}

/** The count on the left, the order on the right. The count is blank while a new answer is on its way. */
@Composable
private fun ResultHeader(total: Long?, sort: LotSort, onSort: (LotSort) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // 14dp above the count and 10 below, plus the 2 the grid starts after.
            .padding(start = Space.gutter, top = 14.dp, end = Space.md, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        AnimatedCount(total) { shown ->
            Text(
                text = shown?.let { "${groupedCount(it)} ${if (it == 1L) "lote" else "lotes"}" }.orEmpty(),
                style = Type.small,
                color = Ink60,
            )
        }
        SortMenu(sort = sort, onSelect = onSort)
    }
}

/** The order, as a native menu: the button shows the current one, the menu lists the six. */
@Composable
private fun SortMenu(sort: LotSort, onSelect: (LotSort) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .touchOverflow(20.dp)
                .heightIn(min = 48.dp)
                .pressClickable(onClick = { open = true })
                .padding(start = 8.dp)
                .semantics { contentDescription = "Ordenar por: ${sort.label}" },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = sort.label, style = Type.label, color = Ink)
            Icon(Icons.Filled.ExpandMore, contentDescription = null, tint = Ink60, modifier = Modifier.size(20.dp))
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            containerColor = Surface,
            shape = Corners.control,
        ) {
            LotSort.entries.forEach { option ->
                val current = option == sort
                DropdownMenuItem(
                    text = { Text(text = option.label, style = Type.label, color = Ink) },
                    trailingIcon = {
                        if (current) Icon(Icons.Filled.Check, contentDescription = null, tint = Ink, modifier = Modifier.size(20.dp))
                    },
                    onClick = {
                        open = false
                        onSelect(option)
                    },
                    modifier = Modifier.semantics {
                        selected = current
                        stateDescription = if (current) "Selecionado" else ""
                    },
                )
            }
        }
    }
}

/**
 * What the empty, focused field offers instead of a blank screen: the last eight searches (kept
 * on the device, with a way to wipe them) and the categories as a way to begin.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecentsPanel(
    recents: List<String>,
    categories: List<Category>,
    onUseRecent: (String) -> Unit,
    onClearRecents: () -> Unit,
    onPickCategory: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(top = Space.sm, bottom = Space.xxl),
    ) {
        if (recents.isNotEmpty()) {
            item(key = "recent-header") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = Space.gutter, end = Space.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Buscas recentes",
                        style = Type.section,
                        color = Ink,
                        modifier = Modifier.semantics { heading() },
                    )
                    QuietButton(label = "Limpar", onClick = onClearRecents)
                }
            }
            items(recents, key = { "recent-$it" }) { text ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .pressClickable(onClick = { onUseRecent(text) }, onClickLabel = "Buscar", press = Press.Surface)
                        .padding(horizontal = Space.gutter),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(Icons.Outlined.History, contentDescription = null, tint = Ink60, modifier = Modifier.size(22.dp))
                    Text(text = text, style = Type.body, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (categories.isNotEmpty()) {
            item(key = "category-header") {
                Text(
                    text = "Categorias",
                    style = Type.section,
                    color = Ink,
                    modifier = Modifier
                        .padding(start = Space.gutter, top = if (recents.isEmpty()) Space.sm else Space.xl, end = Space.gutter)
                        .semantics { heading() },
                )
            }
            item(key = "category-chips") {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Space.gutter),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    categories.forEach { category ->
                        Chip(
                            label = categoryShortLabel(category.name),
                            selected = false,
                            onClick = { onPickCategory(category.externalId) },
                            icon = categoryIcon(category.name),
                        )
                    }
                }
            }
        }
    }
}

/**
 * How a row arrives and leaves: a fade over the standard time, plus a glide to its new place when
 * the rows above change. Keys are the lots' ids, so only new rows fade — a page appended at the
 * end does not replay the rows on screen.
 */
private fun LazyItemScope.arrival(): Modifier =
    Modifier.animateItem(fadeInSpec = Motion.enter(), fadeOutSpec = Motion.exit())
