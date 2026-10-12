package br.com.lote.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.com.lote.BuildConfig
import br.com.lote.data.ThemeMode
import br.com.lote.ui.components.pressClickable
import br.com.lote.ui.components.pressSelectable
import br.com.lote.ui.home.AboutSheet
import br.com.lote.ui.theme.Adaptive
import br.com.lote.ui.theme.AmberText
import br.com.lote.ui.theme.Corners
import br.com.lote.ui.theme.Ink
import br.com.lote.ui.theme.Ink60
import br.com.lote.ui.theme.Line
import br.com.lote.ui.theme.LineControl
import br.com.lote.ui.theme.Paper
import br.com.lote.ui.theme.Space
import br.com.lote.ui.theme.Surface
import br.com.lote.ui.theme.Touch
import br.com.lote.ui.theme.Type

/**
 * The app's settings: the theme, what the app keeps on the phone, and what the app is. There is no
 * account, so there is nothing here about one; every choice is stored on the device and takes
 * effect the moment it is made.
 */
@Composable
fun SettingsScreen(
    theme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    recentSearchCount: Int,
    onClearSearches: () -> Unit,
    totalLots: Long,
    updatedAt: String?,
    onBack: () -> Unit,
) {
    var showAbout by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Paper)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(Touch.target)
                .padding(horizontal = Space.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(Touch.target)
                    .pressClickable(onClick = onBack)
                    .semantics { contentDescription = "Voltar" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Ink, modifier = Modifier.size(24.dp))
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier
                    .widthIn(max = Adaptive.maxContentWidth)
                    .fillMaxWidth()
                    .padding(start = Space.gutter, end = Space.gutter, bottom = Space.x3),
            ) {
                Text(
                    text = "Configurações",
                    style = Type.display,
                    color = Ink,
                    modifier = Modifier.padding(top = 6.dp, bottom = Space.xs).semantics { heading() },
                )

                Section(title = "Aparência", note = "No automático, o Lote acompanha o tema do aparelho.") {
                    Column(Modifier.selectableGroup()) {
                        ThemeMode.entries.forEachIndexed { index, mode ->
                            if (index > 0) Hairline()
                            ThemeRow(mode = mode, selected = mode == theme, onSelect = { onThemeChange(mode) })
                        }
                    }
                }

                Section(
                    title = "Privacidade",
                    note = "Suas buscas e seus favoritos ficam ligados a este aparelho. Não pedimos cadastro.",
                ) {
                    ClearSearchesRow(count = recentSearchCount, onClear = onClearSearches)
                }

                Section(title = "Sobre") {
                    LinkRow(label = "Como funciona", onClick = { showAbout = true })
                    Hairline()
                    ValueRow(label = "Versão", value = BuildConfig.VERSION_NAME)
                }
            }
        }
    }

    if (showAbout) {
        AboutSheet(totalLots = totalLots, updatedAt = updatedAt, onDismiss = { showAbout = false })
    }
}

/** A titled group: its name above, the rows on a card, and an optional note below. */
@Composable
private fun Section(title: String, note: String? = null, rows: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = Space.xxl)) {
        Text(
            text = title,
            style = Type.label,
            color = Ink60,
            modifier = Modifier.padding(start = Space.xs, bottom = Space.sm).semantics { heading() },
        )
        Column(
            Modifier
                .fillMaxWidth()
                .clip(Corners.card)
                .background(Surface)
                .border(1.dp, Line, Corners.card),
        ) {
            rows()
        }
        if (note != null) {
            Text(
                text = note,
                style = Type.small,
                color = Ink60,
                modifier = Modifier.padding(start = Space.xs, top = Space.sm, end = Space.xs),
            )
        }
    }
}

/** One of the three themes: the radio, its name; the whole row is the target. */
@Composable
private fun ThemeRow(mode: ThemeMode, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .pressSelectable(selected = selected, onClick = onSelect)
            .padding(start = Space.xs, end = Space.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The row carries the selection for TalkBack; the radio is only its picture.
        RadioButton(
            selected = selected,
            onClick = null,
            modifier = Modifier.size(Touch.target),
            colors = RadioButtonDefaults.colors(selectedColor = AmberText, unselectedColor = LineControl),
        )
        Text(text = mode.label, style = Type.body, color = Ink, modifier = Modifier.weight(1f))
    }
}

/** Forgets the searches typed in Explore. It says how many there are, and goes quiet at none. */
@Composable
private fun ClearSearchesRow(count: Int, onClear: () -> Unit) {
    val enabled = count > 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .pressClickable(onClick = onClear, enabled = enabled)
            .padding(horizontal = Space.lg, vertical = Space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "Limpar buscas recentes",
                style = Type.body,
                color = Ink,
                modifier = Modifier.alpha(if (enabled) 1f else 0.45f),
            )
            Text(
                text = when (count) {
                    0 -> "Nenhuma busca guardada"
                    1 -> "1 busca guardada neste aparelho"
                    else -> "$count buscas guardadas neste aparelho"
                },
                style = Type.small,
                color = Ink60,
                modifier = Modifier.padding(top = 2.dp).semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

/** A row that opens something: its name and a chevron. */
@Composable
private fun LinkRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .pressClickable(onClick = onClick)
            .padding(start = Space.lg, end = Space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = Type.body, color = Ink, modifier = Modifier.weight(1f))
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = Ink60, modifier = Modifier.size(22.dp))
    }
}

/** A fact, not a control: its name and its value. */
@Composable
private fun ValueRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .semantics(mergeDescendants = true) { }
            .padding(horizontal = Space.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        Text(text = label, style = Type.body, color = Ink, modifier = Modifier.weight(1f))
        Text(text = value, style = Type.body, color = Ink60)
    }
}

/** The rule between two rows, inset from the leading edge as list dividers are. */
@Composable
private fun Hairline() {
    Box(Modifier.fillMaxWidth().padding(start = Space.lg).height(1.dp).background(Line))
}
