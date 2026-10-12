package br.com.loyfus.ui.explore

import br.com.loyfus.core.priceRangeLabel

/**
 * What the filter sheet narrows by: type, deadline and price. Category has its own row of
 * chips and the order its own menu, so neither counts here: the badge on the filter
 * button says how many things the *sheet* is holding.
 */
data class ExploreFilters(
    /** `LEILAO`, `AFTER_MARKET`, or null for both. */
    val modality: String? = null,
    /** Hours until the deadline, or null for any. */
    val endsWithin: Int? = null,
    /** Only lots that take an offer below the asking price. */
    val acceptsProposal: Boolean? = null,
    val minPrice: Long? = null,
    val maxPrice: Long? = null,
) {
    val hasPrice: Boolean get() = minPrice != null || maxPrice != null

    val activeCount: Int
        get() = (if (modality != null) 1 else 0) + (if (endsWithin != null) 1 else 0) +
            (if (acceptsProposal == true) 1 else 0) + (if (hasPrice) 1 else 0)

    /** One removable tag per active filter, in the order of the sheet. */
    fun tags(): List<FilterTag> = buildList {
        modality?.let { code -> TYPES.firstOrNull { it.value == code }?.let { add(FilterTag(FilterKind.Type, it.label)) } }
        endsWithin?.let { hours -> DEADLINES.firstOrNull { it.value == hours }?.let { add(FilterTag(FilterKind.Deadline, it.label)) } }
        if (acceptsProposal == true) add(FilterTag(FilterKind.Offer, "Aceita proposta"))
        priceRangeLabel(minPrice, maxPrice)?.let { add(FilterTag(FilterKind.Price, it)) }
    }

    fun without(kind: FilterKind): ExploreFilters = when (kind) {
        FilterKind.Type -> copy(modality = null)
        FilterKind.Deadline -> copy(endsWithin = null)
        FilterKind.Offer -> copy(acceptsProposal = null)
        FilterKind.Price -> copy(minPrice = null, maxPrice = null)
    }
}

enum class FilterKind { Type, Deadline, Offer, Price }

class FilterTag(val kind: FilterKind, val label: String)

/** An option of a single-choice section of the sheet. */
class Choice<T>(val value: T?, val label: String)

/** A price shortcut: the range it sets, and the words on its chip. */
class PriceShortcut(val label: String, val min: Long?, val max: Long?) {
    fun matches(filters: ExploreFilters) = filters.minPrice == min && filters.maxPrice == max
}

val TYPES = listOf(
    Choice<String>(null, "Todos"),
    Choice("LEILAO", "Leilão"),
    Choice("AFTER_MARKET", "Venda direta"),
)

val DEADLINES = listOf(
    Choice<Int>(null, "Qualquer"),
    Choice(24, "Em 24 h"),
    Choice(72, "Em 3 dias"),
    Choice(168, "Em 7 dias"),
)

val PRICE_SHORTCUTS = listOf(
    PriceShortcut("Até 1 mil", min = null, max = 1_000),
    PriceShortcut("1 – 10 mil", min = 1_000, max = 10_000),
    PriceShortcut("10 – 50 mil", min = 10_000, max = 50_000),
    PriceShortcut("50 – 200 mil", min = 50_000, max = 200_000),
    PriceShortcut("Acima de 200 mil", min = 200_000, max = null),
)
