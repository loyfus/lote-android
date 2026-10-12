package br.com.loyfus.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.com.loyfus.core.groupedCount
import br.com.loyfus.data.LotSort
import br.com.loyfus.ui.components.Chip
import br.com.loyfus.ui.components.PrimaryButton
import br.com.loyfus.ui.components.SecondaryButton
import br.com.loyfus.ui.components.pressClickable
import br.com.loyfus.ui.components.touchOverflow
import br.com.loyfus.ui.theme.Corners
import br.com.loyfus.ui.theme.Ink
import br.com.loyfus.ui.theme.Ink60
import br.com.loyfus.ui.theme.Line
import br.com.loyfus.ui.theme.Paper
import br.com.loyfus.ui.theme.Space
import br.com.loyfus.ui.theme.Touch
import br.com.loyfus.ui.theme.Type
import br.com.loyfus.ui.theme.LineControl
import kotlinx.coroutines.delay

/**
 * The filter sheet: type, deadline, price range and order, held as a draft that only
 * reaches Explore when the reader taps "Mostrar N lotes". N is the live answer: the
 * draft is asked of the API as a one-row query, a beat after the last change, so the
 * button promises what it will deliver. While the answer is on its way it says "Ver
 * resultados" rather than a number it does not have.
 *
 * It opens fully expanded: the footer (Limpar, Mostrar) is part of the sheet and a
 * half-open sheet would leave it below the fold.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(
    initialFilters: ExploreFilters,
    initialSort: LotSort,
    countFor: suspend (ExploreFilters) -> Long?,
    onApply: (ExploreFilters, LotSort) -> Unit,
    onDismiss: () -> Unit,
) {
    var filters by remember { mutableStateOf(initialFilters) }
    var sort by remember { mutableStateOf(initialSort) }
    var count by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(filters) {
        count = null
        delay(COUNT_PAUSE_MILLIS)
        count = countFor(filters)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = Corners.sheet,
        containerColor = Paper,
        contentColor = Ink,
        scrimColor = Color.Black.copy(alpha = 0.45f),
        dragHandle = { SheetHandle() },
    ) {
        Column(Modifier.fillMaxWidth().imePadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Space.gutter, top = 4.dp, end = Space.sm, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Filtros",
                    style = Type.headline,
                    color = Ink,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                Box(
                    modifier = Modifier
                        .touchOverflow(26.dp)
                        .size(Touch.target)
                        .pressClickable(onClick = onDismiss)
                        .semantics { contentDescription = "Fechar" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null, tint = Ink60, modifier = Modifier.size(24.dp))
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Space.gutter),
            ) {
                Section("Tipo") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TYPES.forEach { option ->
                            Chip(
                                label = option.label,
                                selected = filters.modality == option.value,
                                onClick = { filters = filters.copy(modality = option.value) },
                            )
                        }
                    }
                }
                Section("Prazo") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DEADLINES.forEach { option ->
                            Chip(
                                label = option.label,
                                selected = filters.endsWithin == option.value,
                                onClick = { filters = filters.copy(endsWithin = option.value) },
                            )
                        }
                    }
                }
                Section("Faixa de preço") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PriceField(
                            label = "Mínimo",
                            hint = "R$ 0",
                            digits = filters.minPrice?.toString().orEmpty(),
                            onDigitsChange = { filters = filters.copy(minPrice = it.toLongOrNull()) },
                            modifier = Modifier.weight(1f),
                        )
                        PriceField(
                            label = "Máximo",
                            hint = "Sem limite",
                            digits = filters.maxPrice?.toString().orEmpty(),
                            onDigitsChange = { filters = filters.copy(maxPrice = it.toLongOrNull()) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    FlowRow(
                        modifier = Modifier.padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PRICE_SHORTCUTS.forEach { shortcut ->
                            Chip(
                                label = shortcut.label,
                                selected = shortcut.matches(filters),
                                onClick = {
                                    // Tapping the shortcut that is already set clears the range.
                                    filters = if (shortcut.matches(filters)) {
                                        filters.copy(minPrice = null, maxPrice = null)
                                    } else {
                                        filters.copy(minPrice = shortcut.min, maxPrice = shortcut.max)
                                    }
                                },
                            )
                        }
                    }
                }
                Section("Ordenar por") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        LotSort.entries.forEach { option ->
                            Chip(label = option.label, selected = sort == option, onClick = { sort = option })
                        }
                    }
                }
            }

            // The footer is not part of the scroll: what the reader does with the draft
            // is always in reach, under a hairline.
            Box(Modifier.padding(top = Space.lg).fillMaxWidth().height(1.dp).background(Line))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Paper)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(start = Space.gutter, top = Space.lg, end = Space.gutter, bottom = Space.md),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SecondaryButton(
                    label = "Limpar",
                    onClick = {
                        filters = ExploreFilters()
                        sort = LotSort.Ending
                    },
                )
                PrimaryButton(
                    label = count?.let { "Mostrar ${groupedCount(it)} ${if (it == 1L) "lote" else "lotes"}" }
                        ?: "Ver resultados",
                    onClick = { onApply(filters, sort) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(top = 14.dp, bottom = 4.dp)) {
        Text(
            text = title,
            style = Type.cardTitle,
            color = Ink,
            modifier = Modifier.padding(bottom = 10.dp).semantics { heading() },
        )
        content()
    }
}

@Composable
private fun SheetHandle() {
    Box(
        modifier = Modifier
            .padding(top = 8.dp, bottom = 10.dp)
            .size(width = 40.dp, height = 5.dp)
            .background(LineControl.copy(alpha = 0.6f), Corners.tag),
    )
}

private const val COUNT_PAUSE_MILLIS = 300L
