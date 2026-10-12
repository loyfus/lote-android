package br.com.loyfus.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LotSummary(
    val id: Long,
    val sourceCode: String = "",
    val externalId: String = "",
    val lotNumber: String? = null,
    val title: String = "",
    val categoryName: String? = null,
    val categoryExternalId: String? = null,
    val modality: String? = null,
    val modalityLabel: String? = null,
    val organizer: String? = null,
    val currentPrice: Double? = null,
    val initialPrice: Double? = null,
    val minimumIncrement: Double? = null,
    val currency: String = "BRL",
    val bidCount: Int = 0,
    val status: String? = null,
    val statusLabel: String? = null,
    val endingSoon: Boolean = false,
    val endsAt: String? = null,
    val secondsToClose: Long = -1,
    val primaryImageUrl: String? = null,
    val city: String? = null,
    @SerialName("stateCode") val state: String? = null,
    val webUrl: String? = null,
    val acceptsProposal: Boolean = false,
)

@Serializable
data class PageResponse<T>(
    val items: List<T> = emptyList(),
    val page: Int = 0,
    val size: Int = 20,
    val totalElements: Long = 0,
    val totalPages: Int = 0,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
)

@Serializable
data class LotDetail(
    val id: Long,
    val externalId: String = "",
    val sourceCode: String = "",
    val sourceName: String = "",
    val lotNumber: String? = null,
    val title: String = "",
    val description: String? = null,
    val categoryName: String? = null,
    val categoryExternalId: String? = null,
    val modality: String? = null,
    val modalityLabel: String? = null,
    val specs: Map<String, String> = emptyMap(),
    val currentPrice: Double? = null,
    val initialPrice: Double? = null,
    val reservedPrice: Double? = null,
    val estimatedPrice: Double? = null,
    val minimumIncrement: Double? = null,
    val currency: String = "BRL",
    val bidCount: Int = 0,
    val bidderCount: Int = 0,
    val visitCount: Int = 0,
    val status: String? = null,
    // The list endpoint labels the status ("Arrematado"); the detail endpoint only
    // carries the code today, and the screen falls back to its own word for SOLD.
    val statusLabel: String? = null,
    val endingSoon: Boolean = false,
    val sold: Boolean = false,
    val reserved: Boolean = false,
    val acceptsProposal: Boolean = false,
    val isFavourite: Boolean = false,
    val conditionLabel: String? = null,
    val commercialCondition: String? = null,
    val paymentTerms: String? = null,
    val sellerName: String? = null,
    val sellerCity: String? = null,
    val city: String? = null,
    val stateCode: String? = null,
    val startsAt: String? = null,
    val endsAt: String? = null,
    val publishedAt: String? = null,
    // When the crawler last read this lot's price: the price has an age.
    val syncedAt: String? = null,
    val secondsToClose: Long = -1,
    val images: List<String> = emptyList(),
    val webUrl: String? = null,
    val auction: AuctionInfo? = null,
)

@Serializable
data class AuctionInfo(
    val title: String? = null,
    val description: String? = null,
    val modality: String? = null,
    val organizer: String? = null,
    val organizerLogoUrl: String? = null,
    val managerName: String? = null,
    val contactEmail: String? = null,
    val contactPhone: String? = null,
    val startsAt: String? = null,
    val endsAt: String? = null,
    val webUrl: String? = null,
    val noticeUrl: String? = null,
)

@Serializable
data class Category(
    val externalId: String = "",
    val name: String = "",
    val slug: String? = null,
    val parentExternalId: String? = null,
    val level: Int = 0,
    val sourceCount: Int = 0,
    val availableCount: Int = 0,
)

@Serializable
data class MarketplaceStats(
    val totalLots: Long = 0,
    val openLots: Long = 0,
    val endingSoonLots: Long = 0,
    val judicialLots: Long = 0,
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val medianPrice: Double? = null,
    val totalValue: Double? = null,
    val lastRefreshedAt: String? = null,
)

/** One discovery shelf, assembled by the server: what to show and how to open it in Explore. */
@Serializable
data class Shelf(
    val id: String = "",
    val title: String = "",
    val subtitle: String? = null,
    val query: ShelfQuery = ShelfQuery(),
    val items: List<LotSummary> = emptyList(),
)

/** The Explore query behind a shelf's "Ver todos". */
@Serializable
data class ShelfQuery(
    val sort: String = "ending",
    val category: String? = null,
    val modality: String? = null,
    val endsWithin: Int? = null,
    val acceptsProposal: Boolean? = null,
)

@Serializable
data class HomeResponse(
    val updatedAt: String? = null,
    val totalLots: Long = 0,
    /** The nearest window (24, 72 or 168 h) that still has auctions; null when nothing closes in a week. */
    val closing: ClosingWindow? = null,
    /** How many lots take an offer below the asking price. */
    val acceptsProposal: Long = 0,
    val shelves: List<Shelf> = emptyList(),
)

/** "{count} lotes fecham nas próximas {withinHours} h", decided by the server so both clients say the same. */
@Serializable
data class ClosingWindow(
    val count: Long = 0,
    val withinHours: Int = 24,
)
