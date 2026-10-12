package br.com.loyfus.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.loyfus.R
import br.com.loyfus.core.ago
import br.com.loyfus.core.categoryShortLabel
import br.com.loyfus.core.groupedCount
import br.com.loyfus.core.minutesSince
import br.com.loyfus.data.Category
import br.com.loyfus.data.ClosingWindow
import br.com.loyfus.ui.components.Chip
import br.com.loyfus.ui.components.Dot
import br.com.loyfus.ui.components.Press
import br.com.loyfus.ui.components.categoryIcon
import br.com.loyfus.ui.components.pressClickable
import br.com.loyfus.ui.components.touchOverflow
import br.com.loyfus.ui.explore.PRICE_SHORTCUTS
import br.com.loyfus.ui.theme.Amber
import br.com.loyfus.ui.theme.AmberDeep
import br.com.loyfus.ui.theme.AmberSoft
import br.com.loyfus.ui.theme.Glass
import br.com.loyfus.ui.theme.GlassLine
import br.com.loyfus.ui.theme.HeroBottom
import br.com.loyfus.ui.theme.HeroControl
import br.com.loyfus.ui.theme.HeroTop
import br.com.loyfus.ui.theme.Ink
import br.com.loyfus.ui.theme.Ink40
import br.com.loyfus.ui.theme.Ink60
import br.com.loyfus.ui.theme.Line
import br.com.loyfus.ui.theme.LocalReducedMotion
import br.com.loyfus.ui.theme.Motion
import br.com.loyfus.ui.theme.OnAmber
import br.com.loyfus.ui.theme.OnHero
import br.com.loyfus.ui.theme.OnHero80
import br.com.loyfus.ui.theme.Pine
import br.com.loyfus.ui.theme.PineSoft
import br.com.loyfus.ui.theme.Space
import br.com.loyfus.ui.theme.Surface
import br.com.loyfus.ui.theme.Touch
import br.com.loyfus.ui.theme.Type
import br.com.loyfus.ui.theme.sized

/**
 * The gradient behind Home: night blue, top to bottom. It starts at the fixed bar's own
 * colour, so the bar and the hero read as one ground with no seam between them.
 */
@Composable
fun heroBrush(): Brush = Brush.verticalGradient(
    0f to HeroTop,
    1f to HeroBottom,
)

// The sizes the specification names for Home, kept out of the scale: they reuse a
// role's family and weight and change nothing else.
private val pillText = Type.body.sized(17.0, 22.0)
private val shortcutLabel = Type.caption.sized(12.5, 15.0, FontWeight.Medium)
private val bannerTitle = Type.headline.sized(16.0, 21.0)
private val bannerText = Type.small.sized(14.0, 19.0)
private val cardTitle = Type.label.sized(15.0, 19.0)
private val cardValue = Type.priceSticky.sized(21.0, 26.0)
private val cardLegend = Type.small.sized(13.0, 17.0)
private val budgetTitle = Type.headline.sized(18.0, 23.0)
private val budgetChip = Type.label.sized(13.5, 18.0)
private val contextLine = Type.caption.sized(12.5, 16.0, FontWeight.Medium)

/** 4.691 lotes, with the noun that follows the number. */
private fun lots(count: Long): String =
    "${groupedCount(count)} ${if (count == 1L) "lote" else "lotes"}"

/** The door into search, the categories and the one notice worth showing. */
@Composable
fun HomeHero(
    categories: List<Category>,
    showBanner: Boolean,
    onOpenSearch: () -> Unit,
    onOpenCategory: (String) -> Unit,
    onOpenAllCategories: () -> Unit,
    onOpenAbout: () -> Unit,
    onDismissBanner: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(heroBrush())
            .padding(top = Space.xs, bottom = 46.dp),
    ) {
        SearchPill(onClick = onOpenSearch, modifier = Modifier.padding(horizontal = Space.gutter))

        Shortcuts(
            categories = categories,
            onOpenCategory = onOpenCategory,
            onOpenAll = onOpenAllCategories,
            modifier = Modifier.padding(top = 18.dp),
        )

        // Dismissed, the notice fades and the hero closes up behind it, so the sheet
        // rises into the space instead of jumping. With reduced motion it only fades.
        val reducedMotion = LocalReducedMotion.current
        AnimatedVisibility(
            visible = showBanner,
            enter = fadeIn(Motion.enter()) + expandVertically(Motion.enter()),
            exit = if (reducedMotion) fadeOut(Motion.exit()) else fadeOut(Motion.exit(Motion.STANDARD)) + shrinkVertically(Motion.collapse()),
        ) {
            NoticeBanner(
                onOpenAbout = onOpenAbout,
                onDismiss = onDismissBanner,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = Space.gutter),
            )
        }
    }
}

/** The search door: a button, not a field. Tapping it opens Explore with the keyboard. */
@Composable
private fun SearchPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 54.dp)
            .clip(RoundedCornerShape(27.dp))
            .background(Surface)
            // heroControl: 3:1 against both ends of the night blue and against the
            // pill's own fill, so the pill reads as a control in both themes.
            .border(1.dp, HeroControl, RoundedCornerShape(27.dp))
            .pressClickable(onClick = onClick, press = Press.Surface)
            .clearAndSetSemantics { contentDescription = "Buscar lotes" }
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = Ink, modifier = Modifier.size(26.dp))
        Text(
            text = "Buscar carro, imóvel, máquina…",
            style = pillText,
            color = Ink60,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The categories as frosted tiles, and Todas at the end of the row. */
@Composable
private fun Shortcuts(
    categories: List<Category>,
    onOpenCategory: (String) -> Unit,
    onOpenAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = Space.gutter, end = Space.gutter),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(categories, key = { it.externalId }) { category ->
            val label = categoryShortLabel(category.name)
            ShortcutTile(
                label = label,
                icon = categoryIcon(category.name),
                description = label,
                onClick = { onOpenCategory(category.externalId) },
            )
        }
        item(key = "all") {
            ShortcutTile(
                label = "Todas",
                icon = Icons.Outlined.Apps,
                description = "Todas as categorias",
                onClick = onOpenAll,
            )
        }
    }
}

/** A frosted 64dp square with its icon, and the category's name under it. */
@Composable
private fun ShortcutTile(
    label: String,
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            // 74dp, or as wide as the longest word of the label needs: a word is never
            // broken in the middle, however large the reader's text.
            .widthIn(min = 74.dp)
            .width(IntrinsicSize.Min)
            .pressClickable(onClick = onClick)
            .clearAndSetSemantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Glass)
                .border(1.dp, GlassLine, RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = OnHero80, modifier = Modifier.size(30.dp))
        }
        Text(
            text = label,
            style = shortcutLabel,
            color = OnHero80,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** The one notice the app makes, and the only one it can take back. */
@Composable
private fun NoticeBanner(onOpenAbout: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Glass)
            .border(1.dp, GlassLine, RoundedCornerShape(22.dp))
            .padding(start = 14.dp, top = 14.dp, end = 6.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // The brand's own gavel on its amber: the notice is the app saying what it is.
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(Amber),
            contentAlignment = Alignment.Center,
        ) {
            BrandMark(tint = OnAmber, modifier = Modifier.size(22.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .pressClickable(onClick = onOpenAbout, press = Press.Surface)
                .semantics { contentDescription = "Como funciona. Abrir sobre o Loyfus" },
        ) {
            Text(text = "Como funciona", style = bannerTitle, color = OnHero)
            Text(
                text = "Reunimos lotes de leilões do Brasil. O lance é dado no site do leiloeiro.",
                style = bannerText,
                color = OnHero80,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Box(
            modifier = Modifier
                .size(Touch.target)
                .pressClickable(onClick = onDismiss)
                .semantics { contentDescription = "Dispensar aviso" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Close, contentDescription = null, tint = OnHero80, modifier = Modifier.size(20.dp))
        }
    }
}

/** How fresh the catalogue is and how big it is: one line, under the hero. */
@Composable
fun ContextLine(updatedAt: String?, totalLots: Long, modifier: Modifier = Modifier) {
    val fresh = (minutesSince(updatedAt) ?: Long.MAX_VALUE) <= 60
    Row(
        modifier = modifier.padding(start = Space.gutter, end = Space.gutter, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Dot(size = 7.dp, color = if (fresh) Pine else Ink40)
        val stamp = ago(updatedAt)
        Text(
            text = if (stamp != null) "${lots(totalLots)} · atualizado $stamp" else lots(totalLots),
            style = contextLine,
            color = Ink60,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A number the reader can act on: what is closing, and who takes an offer. */
@Composable
fun StatusCard(
    title: String,
    value: String,
    legend: String,
    icon: ImageVector,
    accent: Color,
    onAccent: Color,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Surface)
            .border(1.dp, Line, RoundedCornerShape(22.dp))
            .heightIn(min = 132.dp)
            .pressClickable(onClick = onClick, press = Press.Surface)
            .clearAndSetSemantics { contentDescription = description }
            .padding(14.dp),
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = onAccent, modifier = Modifier.size(22.dp))
        }
        Text(text = title, style = cardTitle, color = Ink, modifier = Modifier.padding(top = 10.dp))
        Text(text = value, style = cardValue, color = Ink, modifier = Modifier.padding(top = 2.dp))
        Spacer(Modifier.weight(1f))
        Text(text = legend, style = cardLegend, color = Ink60, modifier = Modifier.padding(top = 2.dp))
    }
}

/** The two cards side by side. Without a closing window the first one is simply not there. */
@Composable
fun StatusCards(
    closing: ClosingWindow?,
    acceptsProposal: Long,
    onOpenClosing: (ClosingWindow) -> Unit,
    onOpenOffers: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val legend = when (closing?.withinHours) {
        24 -> "nas próximas 24 h"
        72 -> "em até 3 dias"
        else -> "em até 7 dias"
    }
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = Space.gutter),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (closing != null) {
            StatusCard(
                title = "Fecham em breve",
                value = lots(closing.count),
                legend = legend,
                icon = Icons.Outlined.Schedule,
                accent = AmberSoft,
                onAccent = AmberDeep,
                description = "Fecham em breve: ${lots(closing.count)} $legend",
                onClick = { onOpenClosing(closing) },
                modifier = Modifier.weight(1f),
            )
        }
        StatusCard(
            title = "Aceitam proposta",
            value = lots(acceptsProposal),
            legend = "negocie o preço",
            icon = Icons.Outlined.Handshake,
            accent = PineSoft,
            onAccent = Pine,
            description = "Aceitam proposta: ${lots(acceptsProposal)}. Negocie o preço",
            onClick = onOpenOffers,
            modifier = Modifier.weight(1f),
        )
    }
}

/** "Quanto você quer gastar?" and the five bands that answer it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BudgetCard(onPick: (min: Long?, max: Long?) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = Space.gutter, top = 12.dp, end = Space.gutter)
            .clip(RoundedCornerShape(22.dp))
            .background(Surface)
            .border(1.dp, Line, RoundedCornerShape(22.dp))
            .padding(16.dp),
    ) {
        Text(text = "Quanto você quer gastar?", style = budgetTitle, color = Ink)
        FlowRow(
            modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PRICE_SHORTCUTS.forEach { band ->
                Chip(
                    label = band.label,
                    selected = false,
                    onClick = { onPick(band.min, band.max) },
                    height = 38.dp,
                    horizontalPadding = 13.dp,
                    labelStyle = budgetChip,
                    modifier = Modifier.clearAndSetSemantics { contentDescription = "${band.label} reais" },
                )
            }
        }
    }
}

/**
 * The header every phone app has: menu left, mark centre, settings right. It does not scroll —
 * it is the night-blue bar the hero grows out of, so the status bar always sits on the brand's
 * ground. Three 48dp targets, and the mark centred on the bar rather than pushed aside.
 */
@Composable
fun HomeBar(onOpenMenu: () -> Unit, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Touch.target)
            .padding(horizontal = Space.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BarAction(
                icon = Icons.Outlined.Menu,
                description = "Abrir menu",
                onClick = onOpenMenu,
            )
            // There is no account to sign into, so this corner is not an avatar: it is
            // the app's settings, where the theme, the device's data and "Sobre" live.
            BarAction(
                icon = Icons.Outlined.Settings,
                description = "Configurações",
                onClick = onOpenSettings,
            )
        }
        Wordmark(color = OnHero, modifier = Modifier.align(Alignment.Center))
    }
}

/**
 * The brand's gavel over its block, the same geometry as the launcher icon
 * (docs/design-v3/make_icon.py). It is drawn, never typed: there is no glyph for it.
 */
@Composable
fun BrandMark(tint: Color, modifier: Modifier = Modifier) {
    Icon(painterResource(R.drawable.ic_brand_mark), contentDescription = null, tint = tint, modifier = modifier)
}

/**
 * The wordmark: the gavel in amber and LOYFUS in Bricolage. The amber here is the brand, not a
 * call to act — the one place the accent stands for the name instead of for "now".
 */
@Composable
fun Wordmark(color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "Loyfus"
            heading()
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        BrandMark(tint = Amber, modifier = Modifier.size(20.dp))
        Text(text = "LOYFUS", style = Type.wordmark, color = color, modifier = Modifier.clearAndSetSemantics { })
    }
}

/** A bar action on the night blue: the glyph alone, in a 48dp target, with its name. */
@Composable
private fun BarAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(Touch.target)
            .pressClickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = OnHero, modifier = Modifier.size(24.dp))
    }
}

/** What the app is, where the data comes from, what it keeps: the answers to the notice. */
@Composable
fun AboutSections(totalLots: Long, updatedAt: String?) {
    Column(Modifier.fillMaxWidth()) {
        AboutTitle("Como funciona")
        AboutLine("O Loyfus reúne lotes de leilões e vendas diretas do Brasil.")
        AboutLine("O lance, a compra e o pagamento acontecem no site do leiloeiro.")
        AboutLine("Os valores são lidos de tempos em tempos; confirme no site antes de ofertar.")
        Spacer(Modifier.height(Space.xl))
        AboutTitle("Dados")
        val stamp = ago(updatedAt)
        AboutLine(if (stamp != null) "${lots(totalLots)} · atualizado $stamp" else lots(totalLots))
        AboutLine("Fonte: SuperBid")
        Spacer(Modifier.height(Space.xl))
        AboutTitle("Privacidade")
        AboutLine("Seus favoritos ficam ligados a este aparelho. Não pedimos cadastro.")
    }
}

@Composable
private fun AboutTitle(text: String) {
    Text(
        text = text,
        style = Type.cardTitle,
        color = Ink,
        modifier = Modifier.padding(bottom = 6.dp).semantics { heading() },
    )
}

@Composable
private fun AboutLine(text: String) {
    Text(text = text, style = Type.small, color = Ink60, modifier = Modifier.padding(bottom = 4.dp))
}
