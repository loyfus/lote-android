package br.com.loyfus.data

/** The single entry point the UI uses to reach the catalogue. */
class LoyfusRepository(private val api: LoyfusApiClient) {

    private val parser = LoyfusApiClient.parser

    /** The discovery screen in one request: shelves are assembled by the server. */
    suspend fun home(): HomeResponse =
        parser.decodeFromString(api.getRaw("api/v1/home"))

    suspend fun categories(): List<Category> =
        parser.decodeFromString(api.getRaw("api/v1/categories"))

    suspend fun lots(query: LotQuery): PageResponse<LotSummary> =
        parser.decodeFromString(api.getRaw("api/v1/lots", query.toParams()))

    /** Every saved lot of this device, newest page first. Pages are walked until the last. */
    suspend fun favourites(): List<LotSummary> {
        val all = mutableListOf<LotSummary>()
        var page = 0
        while (page < MAX_FAVOURITE_PAGES) {
            val response: PageResponse<LotSummary> = parser.decodeFromString(
                api.getRaw("api/v1/favourites", mapOf("page" to "$page", "size" to "$FAVOURITE_PAGE"))
            )
            all += response.items
            if (!response.hasNext) break
            page++
        }
        return all
    }

    suspend fun lot(id: Long): LotDetail =
        parser.decodeFromString(api.getRaw("api/v1/lots/$id"))

    suspend fun addFavourite(lotId: Long) = api.post("api/v1/favourites/$lotId")

    suspend fun removeFavourite(lotId: Long) = api.delete("api/v1/favourites/$lotId")

    private companion object {
        // The API caps a page at 60; a device with more than a thousand saved lots is a bug, not a user.
        const val FAVOURITE_PAGE = 60
        const val MAX_FAVOURITE_PAGES = 20
    }
}

/** The six orderings of the browse screen: the API's parameter and the label shown. */
enum class LotSort(val param: String, val label: String) {
    Ending("ending", "Encerrando primeiro"),
    PriceAsc("price_asc", "Menor preço"),
    PriceDesc("price_desc", "Maior preço"),
    Bids("bids", "Mais lances"),
    Recent("recent", "Mais recentes"),
    Popular("popular", "Mais vistos");

    companion object {
        /** An unknown parameter falls back to the default rather than breaking the screen. */
        fun of(param: String?): LotSort = entries.firstOrNull { it.param == param } ?: Ending
    }
}

/** Everything the browse screen can narrow by, in one value. */
data class LotQuery(
    val search: String? = null,
    val category: String? = null,
    val modality: String? = null,
    /** Hours until the deadline. */
    val endsWithin: Int? = null,
    /** Only lots that take an offer below the asking price. */
    val acceptsProposal: Boolean? = null,
    val minPrice: Long? = null,
    val maxPrice: Long? = null,
    val sort: LotSort = LotSort.Ending,
    val page: Int = 0,
    val size: Int = 20,
) {
    fun toParams(): Map<String, String?> = mapOf(
        "q" to search?.trim()?.takeIf { it.isNotEmpty() },
        "category" to category,
        "modality" to modality,
        "endsWithin" to endsWithin?.toString(),
        "acceptsProposal" to acceptsProposal?.toString(),
        "minPrice" to minPrice?.toString(),
        "maxPrice" to maxPrice?.toString(),
        "sort" to sort.param,
        "page" to "$page",
        "size" to "$size",
    )
}
