package br.com.lote.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import br.com.lote.core.LotKind
import br.com.lote.core.Urgency
import br.com.lote.core.priceShort
import br.com.lote.core.stickyCaption
import br.com.lote.core.urgencyOf
import br.com.lote.ui.components.PrimaryButton
import br.com.lote.ui.theme.Ink
import br.com.lote.ui.theme.Ink60
import br.com.lote.ui.theme.Adaptive
import br.com.lote.ui.theme.Line
import br.com.lote.ui.theme.Paper
import br.com.lote.ui.theme.Type
import br.com.lote.ui.theme.AmberText
import br.com.lote.ui.theme.sized
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color

/**
 * The call to action: price on the left, the way to the source on the right, on every screen of a
 * lot and always in reach. The wording follows what the lot is — "Dar lance no site" for a
 * running auction, "Ver oferta no site" for a direct sale, "Ver no site" once over — and with
 * no link it is a disabled "Link indisponível" rather than a button that would do nothing. The
 * bid happens at the auctioneer, which is why the button leaves the app.
 */
@Composable
fun StickyBar(
    model: DetailModel,
    seconds: Long,
    onOpenSource: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lot = model.lot
    val urgency = urgencyOf(model.kind, seconds, hasDeadline = lot.endsAt != null)
    val over = urgency == Urgency.Ended || model.sold
    val url = model.sourceUrl
    val label = when {
        url == null -> "Link indisponível"
        over -> "Ver no site"
        model.kind == LotKind.DirectSale -> "Ver oferta no site"
        else -> "Dar lance no site"
    }

    Column(modifier.fillMaxWidth().background(Paper.copy(alpha = 0.94f))) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .widthIn(max = Adaptive.maxContentWidth)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 12.dp),
        ) {
            PriceAndAction(
                price = {
                    Column {
                        Text(
                            text = priceShort(lot.currentPrice),
                            style = Type.priceSticky,
                            color = if (urgency == Urgency.Hot) AmberText else Ink,
                            maxLines = 1,
                            softWrap = false,
                        )
                        Text(
                            text = stickyCaption(model.kind, urgency, lot.bidCount, seconds),
                            style = Type.caption.sized(12.5, 16.0),
                            color = Ink60,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                },
                action = {
                    PrimaryButton(
                        label = label,
                        onClick = { url?.let(onOpenSource) },
                        icon = if (url == null) null else Icons.AutoMirrored.Outlined.OpenInNew,
                        enabled = url != null,
                    )
                },
            )
        }
    }
}

/**
 * Price beside the button when they fit, above it when they do not: at a large font scale the
 * figure and a full-width label cannot share a line, and the figure keeps its line.
 */
@Composable
private fun PriceAndAction(price: @Composable () -> Unit, action: @Composable () -> Unit) {
    Layout(content = { price(); action() }) { measurables, constraints ->
        val gap = 16.dp.roundToPx()
        val stack = 12.dp.roundToPx()
        val (priceItem, actionItem) = measurables
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val priceWidth = priceItem.maxIntrinsicWidth(Constraints.Infinity)
        val actionWidth = actionItem.maxIntrinsicWidth(Constraints.Infinity)
        val sideBySide = priceWidth + gap + actionWidth <= constraints.maxWidth

        if (sideBySide) {
            val priceBox = priceItem.measure(loose.copy(maxWidth = priceWidth))
            val actionBox = actionItem.measure(Constraints.fixedWidth(constraints.maxWidth - priceWidth - gap))
            val height = maxOf(priceBox.height, actionBox.height)
            layout(constraints.maxWidth, height) {
                priceBox.placeRelative(0, (height - priceBox.height) / 2)
                actionBox.placeRelative(constraints.maxWidth - actionBox.width, (height - actionBox.height) / 2)
            }
        } else {
            val priceBox = priceItem.measure(loose)
            val actionBox = actionItem.measure(Constraints.fixedWidth(constraints.maxWidth))
            layout(constraints.maxWidth, priceBox.height + stack + actionBox.height) {
                priceBox.placeRelative(0, 0)
                actionBox.placeRelative(0, priceBox.height + stack)
            }
        }
    }
}
