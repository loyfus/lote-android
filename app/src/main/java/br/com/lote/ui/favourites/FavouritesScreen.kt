package br.com.lote.ui.favourites

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.lote.core.SavedGroup
import br.com.lote.core.savedGroupOf
import br.com.lote.ui.components.AnimatedCount
import br.com.lote.ui.components.Dot
import br.com.lote.ui.components.EmptyState
import br.com.lote.ui.components.ErrorState
import br.com.lote.ui.components.LotCardModel
import br.com.lote.ui.components.PrimaryButton
import br.com.lote.ui.components.SavedRow
import br.com.lote.ui.components.SavedSkeleton
import br.com.lote.ui.theme.AmberText
import br.com.lote.ui.theme.Ink
import br.com.lote.ui.theme.Ink60
import br.com.lote.ui.theme.LineControl
import br.com.lote.ui.theme.Motion
import br.com.lote.ui.theme.Paper
import br.com.lote.ui.theme.Space
import br.com.lote.ui.theme.Type
import br.com.lote.ui.theme.sized
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** How long "Desfazer" stays on screen. */
private const val UNDO_MILLIS = 5_000L

/**
 * What the reader saved, ordered by what needs them first: closing today, then this week, then
 * the rest, then the ones already over (dimmed). Removing a lot is one tap on its heart and is
 * never final for five seconds: the row goes at once and a snackbar offers to put it back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouritesScreen(
    viewModel: FavouritesViewModel,
    snackbarHost: SnackbarHostState,
    onOpenLot: (Long) -> Unit,
    onExplore: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val accessibility = LocalAccessibilityManager.current

    // The list is read again every time the tab is opened: a heart tapped in Explore
    // since the last visit has to be here, and the clock on every row has moved.
    LaunchedEffect(Unit) { viewModel.refresh(manual = false) }

    val visible = remember(state.items, state.ids) { state.items.filter { it.id in state.ids } }
    val groups = remember(visible) { groupSaved(visible) }

    fun remove(model: LotCardModel) {
        viewModel.set(model.id, false)
        scope.launch {
            // A newer removal replaces the one on screen instead of queueing behind it.
            snackbarHost.currentSnackbarData?.dismiss()
            // The system may lengthen the five seconds for a reader who needs more time.
            val millis = accessibility?.calculateRecommendedTimeoutMillis(
                UNDO_MILLIS, containsIcons = false, containsText = true, containsControls = true,
            ) ?: UNDO_MILLIS
            val result = withTimeoutOrNull(millis) {
                snackbarHost.showSnackbar(
                    message = "Removido dos salvos",
                    actionLabel = "Desfazer",
                    withDismissAction = false,
                    duration = SnackbarDuration.Indefinite,
                )
            }
            if (result == SnackbarResult.ActionPerformed) viewModel.restore(model)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Paper)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        val phase = when {
            state.loaded -> ScreenPhase.Ready
            state.error != null -> ScreenPhase.Failed
            else -> ScreenPhase.Loading
        }
        AnimatedContent(targetState = phase, transitionSpec = { Motion.contentSwap }, label = "saved") { shown ->
            when (shown) {
                ScreenPhase.Failed -> ErrorState(
                    message = state.error.orEmpty(),
                    onRetry = { viewModel.refresh(manual = false) },
                    modifier = Modifier.fillMaxSize(),
                )
                ScreenPhase.Loading -> SavedSkeleton()
                ScreenPhase.Ready -> PullToRefreshBox(
                    isRefreshing = state.pulling,
                    onRefresh = { viewModel.refresh(manual = true) },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = Space.x3),
                    ) {
                        item(key = "title") { Title(count = visible.size) }
                        if (visible.isEmpty()) {
                            item(key = "empty") {
                                EmptyState(
                                    icon = Icons.Outlined.FavoriteBorder,
                                    title = "Nada guardado ainda",
                                    message = "Toque no coração de um lote para acompanhar o prazo por aqui.",
                                    modifier = Modifier.padding(top = Space.x4),
                                    action = { PrimaryButton(label = "Explorar lotes", onClick = onExplore) },
                                )
                            }
                        }
                        groups.forEach { (group, rows) ->
                            item(key = "group-${group.name}") { GroupHeader(group = group, count = rows.size) }
                            items(rows, key = { it.id }) { model ->
                                SavedRow(
                                    model = model,
                                    dimmed = group == SavedGroup.Ended,
                                    onOpen = { onOpenLot(model.id) },
                                    onRemove = { remove(model) },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun groupSaved(models: List<LotCardModel>): List<Pair<SavedGroup, List<LotCardModel>>> =
    models
        .groupBy { savedGroupOf(it.kind, it.urgency, it.secondsToClose) }
        .map { (group, rows) ->
            // Soonest first; a lot with no deadline has nothing to be soonest by.
            group to rows.sortedBy { if (it.secondsToClose > 0) it.secondsToClose else Long.MAX_VALUE }
        }
        .sortedBy { it.first.ordinal }

@Composable
private fun Title(count: Int) {
    Column(Modifier.fillMaxWidth().padding(start = Space.gutter, top = 6.dp, end = Space.gutter, bottom = 8.dp)) {
        Text(text = "Salvos", style = Type.display, color = Ink, modifier = Modifier.semantics { heading() })
        if (count > 0) {
            AnimatedCount(count.toLong(), modifier = Modifier.padding(top = 4.dp)) { shown ->
                val n = shown ?: 0L
                Text(
                    text = "$n ${if (n == 1L) "lote" else "lotes"} neste aparelho",
                    style = Type.small,
                    color = Ink60,
                )
            }
        }
    }
}

/** The dot is the colour key of the group: amber for what closes today, a neutral for the rest. */
@Composable
private fun GroupHeader(group: SavedGroup, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = Space.gutter, top = 22.dp, end = Space.gutter, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Dot(size = 8.dp, color = if (group == SavedGroup.Today) AmberText else LineControl)
        Text(
            text = group.title,
            style = Type.label.sized(14.0, 20.0),
            color = Ink,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        Text(text = count.toString(), style = Type.small.sized(13.0, 18.0), color = Ink60)
    }
}

/** Which of its three faces the screen shows; the cross-fade runs only when this changes. */
private enum class ScreenPhase { Loading, Failed, Ready }
