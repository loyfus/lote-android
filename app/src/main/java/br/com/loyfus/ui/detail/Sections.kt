package br.com.loyfus.ui.detail

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.loyfus.core.LotKind
import br.com.loyfus.core.ago
import br.com.loyfus.core.clockTime
import br.com.loyfus.core.endLabel
import br.com.loyfus.data.AuctionInfo
import br.com.loyfus.ui.components.QuietButton
import br.com.loyfus.ui.components.SecondaryButton
import br.com.loyfus.ui.components.categoryIcon
import br.com.loyfus.ui.theme.AmberDeep
import br.com.loyfus.ui.theme.AmberSoft
import br.com.loyfus.ui.theme.Corners
import br.com.loyfus.ui.theme.Ink
import br.com.loyfus.ui.theme.Ink60
import br.com.loyfus.ui.theme.Ink80
import br.com.loyfus.ui.theme.Line
import br.com.loyfus.ui.theme.Motion
import br.com.loyfus.ui.theme.PaperDim
import br.com.loyfus.ui.theme.Pine
import br.com.loyfus.ui.theme.PineSoft
import br.com.loyfus.ui.theme.Surface
import br.com.loyfus.ui.theme.Type
import br.com.loyfus.ui.theme.sized

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = Type.section,
        color = Ink,
        modifier = modifier.padding(bottom = 12.dp).semantics { heading() },
    )
}

/**
 * The tags above the title: what the lot is (category, kind, condition) and the two facts
 * worth a glance before reading on — that it accepts a proposal, that it was sold.
 * Neutral by default; colour only where it carries meaning.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Kicker(model: DetailModel, modifier: Modifier = Modifier) {
    val lot = model.lot
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        model.categoryLabel?.let { label ->
            KickerTag(label, icon = lot.categoryName?.let(::categoryIcon))
        }
        KickerTag(if (model.kind == LotKind.DirectSale) "Venda direta" else "Leilão")
        lot.conditionLabel?.takeIf { it.isNotBlank() }?.let { KickerTag(it) }
        if (lot.acceptsProposal) {
            KickerTag("Aceita proposta", icon = Icons.Outlined.Handshake, background = PineSoft, content = Pine)
        }
        model.soldLabel?.let { KickerTag(it, background = AmberSoft, content = AmberDeep) }
    }
}

@Composable
private fun KickerTag(
    label: String,
    icon: ImageVector? = null,
    background: Color = PaperDim,
    content: Color = Ink80,
) {
    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = 28.dp)
            .clip(Corners.tag)
            .background(background)
            .padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
        }
        Text(text = label, style = Type.label.sized(13.0, 18.0), color = content)
    }
}

/**
 * Where the numbers come from and how old they are: the crawler reads the catalogue every
 * half hour and the real bid changes by the second, so the lot says when its value was read
 * and sends the reader to the source to confirm.
 */
@Composable
fun SourceNote(model: DetailModel, modifier: Modifier = Modifier) {
    val lot = model.lot
    val instruction = if (model.kind == LotKind.DirectSale) {
        "A compra é feita no site do vendedor; confirme o preço lá."
    } else {
        "O lance é dado no site do leiloeiro; confirme o valor atual lá."
    }
    val read = clockTime(lot.syncedAt)
    val age = ago(lot.syncedAt)
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = Ink60, modifier = Modifier.size(20.dp))
        Text(
            text = if (read != null && age != null) "Valores lidos às $read ($age). $instruction" else instruction,
            style = Type.small.sized(13.5, 19.0),
            color = Ink60,
        )
    }
}

/** The four first entries of the ficha, and the commission, in two columns. */
@Composable
fun Summary(facts: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    if (facts.isEmpty()) return
    Column(modifier.fillMaxWidth()) {
        SectionTitle("Resumo")
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            facts.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    pair.forEach { (label, value) ->
                        Column(Modifier.weight(1f)) {
                            Text(text = label, style = Type.caption.sized(12.5, 16.0), color = Ink60)
                            Text(
                                text = value,
                                style = Type.body.sized(16.0, 22.0, FontWeight.SemiBold),
                                color = Ink,
                                modifier = Modifier.padding(top = 1.dp),
                            )
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** The lot's description, five lines of it, and a way to the rest. Never "Ler edital": it is the same text. */
@Composable
fun Description(text: String, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var overflows by remember { mutableStateOf(false) }
    // The text opens and closes over 200 ms instead of jumping a screen's worth.
    Column(modifier.fillMaxWidth().animateContentSize(Motion.enter())) {
        SectionTitle("Descrição")
        Text(
            text = text,
            style = Type.bodyDense,
            color = Ink80,
            maxLines = if (expanded) Int.MAX_VALUE else 5,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow },
        )
        if (overflows || expanded) {
            QuietButton(
                label = if (expanded) "Mostrar menos" else "Ler mais",
                onClick = { expanded = !expanded },
                icon = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** How many rows of the ficha show before "Ver todas". */
private const val SPEC_ROWS = 8

/** The ficha técnica: every attribute the source gave, in reading order, with a hairline between rows. */
@Composable
fun Specs(specs: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    if (specs.isEmpty()) return
    var all by rememberSaveable { mutableStateOf(false) }
    val shown = if (all || specs.size <= SPEC_ROWS) specs else specs.take(SPEC_ROWS)
    Column(modifier.fillMaxWidth().animateContentSize(Motion.enter())) {
        SectionTitle("Ficha técnica")
        shown.forEachIndexed { index, (label, value) ->
            if (index > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
            DefinitionRow(label, value, Modifier.padding(vertical = 10.dp))
        }
        if (specs.size > SPEC_ROWS) {
            QuietButton(
                label = if (all) "Mostrar menos" else "Ver todas (${specs.size})",
                onClick = { all = !all },
                icon = if (all) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** A label on the left in grey and its value on the right, heavier: the shape of every row of data here. */
@Composable
private fun DefinitionRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = label, style = Type.small, color = Ink60, modifier = Modifier.weight(0.9f))
        Text(
            text = value,
            style = Type.small.copy(fontWeight = FontWeight.SemiBold),
            color = Ink,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.1f),
        )
    }
}

/** The purchase conditions, one per line. The reserve price and payment terms never appear here. */
@Composable
fun Conditions(items: List<String>, modifier: Modifier = Modifier) {
    if (items.isEmpty()) return
    Column(modifier.fillMaxWidth()) {
        SectionTitle("Condições de compra")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { line ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier
                            .padding(top = 9.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Ink60),
                    )
                    Text(text = line, style = Type.bodyDense, color = Ink80)
                }
            }
        }
    }
}

/**
 * The auction the lot belongs to. The place is labelled "Local do leilão" on purpose:
 * the catalogue's city and state are the address of the event, not of the thing sold, and
 * anything else would be false for nearly every lot.
 */
@Composable
fun AuctionCard(
    auction: AuctionInfo?,
    city: String?,
    state: String?,
    onOpenUrl: (String) -> Unit,
    onMail: (String) -> Unit,
    onDial: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (auction == null) return
    val period = listOfNotNull(endLabel(auction.startsAt), endLabel(auction.endsAt))
        .takeIf { it.size == 2 }?.joinToString(" → ")
    val place = listOfNotNull(city?.takeIf { it.isNotBlank() }, state?.takeIf { it.isNotBlank() }).joinToString(" - ")
    val email = auction.contactEmail?.takeIf { it.isNotBlank() }
    val phone = auction.contactPhone?.takeIf { it.isNotBlank() }

    Column(modifier.fillMaxWidth()) {
        SectionTitle("Sobre o leilão")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Corners.card)
                .background(Surface)
                .border(1.dp, Line, Corners.card)
                .padding(16.dp),
        ) {
            auction.title?.takeIf { it.isNotBlank() }?.let {
                Text(text = it, style = Type.cardTitle, color = Ink, modifier = Modifier.padding(bottom = 6.dp))
            }
            auction.organizer?.takeIf { it.isNotBlank() }?.let { DefinitionRow("Leiloeiro", it, Modifier.padding(vertical = 7.dp)) }
            auction.managerName?.takeIf { it.isNotBlank() }?.let { DefinitionRow("Responsável", it, Modifier.padding(vertical = 7.dp)) }
            period?.let { DefinitionRow("Período", it, Modifier.padding(vertical = 7.dp)) }
            if (place.isNotEmpty()) DefinitionRow("Local do leilão", place, Modifier.padding(vertical = 7.dp))
            if (email != null || phone != null) {
                Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(text = "Contato", style = Type.small, color = Ink60, modifier = Modifier.weight(0.9f))
                    Column(Modifier.weight(1.1f), horizontalAlignment = Alignment.End) {
                        email?.let { ContactLine(it, onClick = { onMail(it) }) }
                        phone?.let { ContactLine(it, onClick = { onDial(it) }) }
                    }
                }
            }
            auction.noticeUrl?.takeIf { it.isNotBlank() }?.let { url ->
                SecondaryButton(
                    label = "Abrir edital (PDF)",
                    onClick = { onOpenUrl(url) },
                    icon = Icons.Outlined.PictureAsPdf,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
            }
        }
    }
}

/** A tappable value: underlined ink, not a coloured link, in a 48dp-high target. */
@Composable
private fun ContactLine(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Text(
            text = text,
            style = Type.small.copy(fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline),
            color = Ink,
            textAlign = TextAlign.End,
        )
    }
}
