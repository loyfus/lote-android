package br.com.loyfus.ui.components

import br.com.loyfus.core.LotKind
import br.com.loyfus.core.Urgency
import br.com.loyfus.core.bids
import br.com.loyfus.core.kindOf
import br.com.loyfus.core.place
import br.com.loyfus.core.priceShort
import br.com.loyfus.core.timeLeft
import br.com.loyfus.core.tidyTitle
import br.com.loyfus.core.urgencyOf
import br.com.loyfus.data.LotSummary

/** What a time chip on a photograph says and how loudly. */
class ChipSpec(val text: String, val hot: Boolean, val clock: Boolean)

/**
 * Everything a tile or saved row displays, worked out once when the lot arrives rather than on
 * every recomposition of a scrolling list: the cleaned title runs a dozen regexes and a feed
 * redraws it sixty times a second.
 *
 * The data model is untouched. The deadline is the snapshot of the moment the page was read,
 * which is why a feed carries a freshness label and a pull to refresh, not a clock per card.
 */
class LotCardModel(
    val lot: LotSummary,
    val title: String,
    val place: String?,
    val kind: LotKind,
    val urgency: Urgency,
    val price: String,
    val caption: String?,
    val chip: ChipSpec?,
    /** Reading of the whole card for a screen reader: the heart stays a separate stop. */
    val label: String,
) {
    val id: Long get() = lot.id
    val hot: Boolean get() = urgency == Urgency.Hot
    val secondsToClose: Long get() = lot.secondsToClose
}

fun LotSummary.toCardModel(): LotCardModel {
    val tidy = tidyTitle(title)
    val kind = kindOf(modality)
    val urgency = urgencyOf(kind, secondsToClose, hasDeadline = endsAt != null)
    val price = priceShort(currentPrice)

    val caption = when (kind) {
        LotKind.Auction -> bids(bidCount)
        LotKind.DirectSale -> if (acceptsProposal) "Aceita proposta" else null
    }
    val chip = when {
        urgency == Urgency.Ended -> ChipSpec("Encerrado", hot = false, clock = true)
        kind == LotKind.DirectSale -> ChipSpec("Venda direta", hot = false, clock = false)
        urgency == Urgency.Hot -> ChipSpec(timeLeft(secondsToClose), hot = true, clock = true)
        urgency == Urgency.Normal -> ChipSpec(timeLeft(secondsToClose), hot = false, clock = true)
        else -> null
    }

    val spoken = buildList {
        add(tidy.headline)
        add(price)
        when (kind) {
            LotKind.Auction -> add(bids(bidCount))
            LotKind.DirectSale -> {
                add("venda direta")
                if (acceptsProposal) add("aceita proposta")
            }
        }
        when {
            urgency == Urgency.Ended -> add("encerrado")
            urgency == Urgency.None -> Unit
            kind == LotKind.DirectSale -> add("disponível por ${timeLeft(secondsToClose)}")
            else -> add("encerra em ${timeLeft(secondsToClose)}")
        }
    }.joinToString(", ")

    return LotCardModel(
        lot = this,
        title = tidy.headline,
        place = place(tidy.detail),
        kind = kind,
        urgency = urgency,
        price = price,
        caption = caption,
        chip = chip,
        label = spoken,
    )
}
