package br.com.loyfus.ui.detail

import android.os.SystemClock
import br.com.loyfus.core.LotKind
import br.com.loyfus.core.categoryShortLabel
import br.com.loyfus.core.commissionOf
import br.com.loyfus.core.kindOf
import br.com.loyfus.core.purchaseConditions
import br.com.loyfus.core.specOrder
import br.com.loyfus.core.tidyTitle
import br.com.loyfus.data.LotDetail

/** A lot with every display value worked out once, when it arrives. */
class DetailModel(
    val lot: LotDetail,
    /** The cleaned title; the detail the source wrote after the first " | " sits beneath it. */
    val title: String,
    val titleDetail: String?,
    val kind: LotKind,
    /** The ficha técnica in reading order. */
    val specs: List<Pair<String, String>>,
    /** The four first entries of the ficha, and the commission when the lot has one. */
    val summary: List<Pair<String, String>>,
    /** `commercialCondition` one item per line. The reserve price and payment terms are never shown. */
    val conditions: List<String>,
    val categoryLabel: String?,
    val sold: Boolean,
    /** The API's label for the status ("Arrematado"), shown beside the kind of a sold lot. */
    val soldLabel: String?,
    /** `webUrl` of the lot at the source, when it has one. */
    val sourceUrl: String?,
    /** The device clock at the moment the lot was read: the deadline ticks from here. */
    val readAtElapsed: Long,
) {
    val id: Long get() = lot.id

    /** The text a share sends: "title — link". */
    val shareText: String get() = lot.webUrl?.let { "$title — $it" } ?: title

    /** A long title is set smaller so it does not take the first screen by itself. */
    val longTitle: Boolean get() = title.length > LONG_TITLE
}

private const val LONG_TITLE = 70

/** The label of a sold lot, when the detail endpoint carries only the status code. */
private const val SOLD_LABEL = "Arrematado"

fun LotDetail.toDetailModel(): DetailModel {
    val tidy = tidyTitle(title)
    val specs = specOrder(specs)
    val conditions = purchaseConditions(commercialCondition)
    val summary = specs.take(4) + listOfNotNull(commissionOf(conditions))
    val sold = status == "SOLD"
    return DetailModel(
        lot = this,
        title = tidy.headline,
        titleDetail = tidy.detail,
        kind = kindOf(modality),
        specs = specs,
        summary = summary,
        conditions = conditions,
        categoryLabel = categoryName?.let(::categoryShortLabel),
        sold = sold,
        soldLabel = if (sold) statusLabel ?: SOLD_LABEL else null,
        sourceUrl = webUrl?.takeIf { it.isNotBlank() },
        readAtElapsed = SystemClock.elapsedRealtime(),
    )
}
