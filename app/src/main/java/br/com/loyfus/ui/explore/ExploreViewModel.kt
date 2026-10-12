package br.com.loyfus.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.loyfus.data.Category
import br.com.loyfus.data.LotQuery
import br.com.loyfus.data.LotSort
import br.com.loyfus.data.LoyfusRepository
import br.com.loyfus.data.RecentSearchesStore
import br.com.loyfus.data.ShelfQuery
import br.com.loyfus.data.attempt
import br.com.loyfus.data.userMessage
import br.com.loyfus.ui.components.LotCardModel
import br.com.loyfus.ui.components.toCardModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ExploreUiState(
    /** What is in the search field. */
    val query: String = "",
    /** The search the results below reflect: the field's text, after the pause. */
    val committed: String = "",
    val category: String? = null,
    val filters: ExploreFilters = ExploreFilters(),
    val sort: LotSort = LotSort.Ending,
    val categories: List<Category> = emptyList(),
    val items: List<LotCardModel> = emptyList(),
    /** Lots matching the current search and filters. */
    val total: Long? = null,
    /** Lots in the whole catalogue: the field's placeholder promises this many. */
    val catalogueTotal: Long? = null,
    val page: Int = 0,
    val hasNext: Boolean = false,
    /** The first page is in flight and there is nothing to show yet: a skeleton. */
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    /** The reader pulled the grid down. */
    val pulling: Boolean = false,
    val error: String? = null,
    /** A later page failed: the grid offers a retry instead of asking again by itself. */
    val moreFailed: Boolean = false,
    val recents: List<String> = emptyList(),
    /** Bumped when another screen wants the keyboard up in the search field. */
    val focusRequest: Int = 0,
    /** Bumped when the question changes: new answers start at the top of the list. */
    val resetToken: Int = 0,
)

/**
 * The state of the browse screen: what is searched, filtered and sorted, and the lots that came
 * back. Held above the navigation graph so it survives a trip to a lot and a switch of tab.
 *
 * Every change of the question starts a new read and cancels the one in flight, so an answer to
 * a question the reader already replaced can never land on the screen.
 */
class ExploreViewModel(
    private val repository: LoyfusRepository,
    private val recentSearches: RecentSearchesStore,
) : ViewModel() {

    private val _state = MutableStateFlow(ExploreUiState(recents = recentSearches.load()))
    val state: StateFlow<ExploreUiState> = _state.asStateFlow()

    private var searchJob: Job? = null
    private var loadJob: Job? = null
    private var moreJob: Job? = null

    init {
        loadCategories()
        viewModelScope.launch {
            attempt { repository.lots(LotQuery(size = 1)).totalElements }
                .onSuccess { total -> _state.update { it.copy(catalogueTotal = total) } }
        }
        reload(clear = true)
    }

    // ---- the question ----

    fun onQueryChange(text: String) {
        _state.update { it.copy(query = text) }
        searchJob?.cancel()
        val wanted = text.trim()
        if (wanted == _state.value.committed) return
        searchJob = viewModelScope.launch {
            // The pause lets a word be finished before it is searched; clearing the
            // field needs no such wait.
            if (wanted.isNotEmpty()) delay(SEARCH_PAUSE_MILLIS)
            commit(wanted)
        }
    }

    /** The keyboard's search key: search now, and remember it. */
    fun submit() {
        searchJob?.cancel()
        val wanted = _state.value.query.trim()
        remember(wanted)
        if (wanted != _state.value.committed) commit(wanted)
    }

    fun clearSearch() {
        searchJob?.cancel()
        _state.update { it.copy(query = "") }
        if (_state.value.committed.isNotEmpty()) commit("")
    }

    /** Runs a remembered search. */
    fun useRecent(text: String) {
        searchJob?.cancel()
        _state.update { it.copy(query = text) }
        remember(text)
        commit(text.trim())
    }

    /** Called when a lot is opened from the results: a search that found something is worth keeping. */
    fun rememberCurrentSearch() = remember(_state.value.committed)

    fun clearRecents() {
        recentSearches.clear()
        _state.update { it.copy(recents = emptyList()) }
    }

    fun selectCategory(id: String?) {
        if (id == _state.value.category) return
        _state.update { it.copy(category = id) }
        reload(clear = true)
    }

    fun applyFilters(filters: ExploreFilters, sort: LotSort) {
        _state.update { it.copy(filters = filters, sort = sort) }
        reload(clear = true)
    }

    fun removeFilter(kind: FilterKind) {
        _state.update { it.copy(filters = it.filters.without(kind)) }
        reload(clear = true)
    }

    fun selectSort(sort: LotSort) {
        if (sort == _state.value.sort) return
        _state.update { it.copy(sort = sort) }
        reload(clear = true)
    }

    /** The way out of "Nenhum lote encontrado": start the question over. */
    fun clearAll() {
        searchJob?.cancel()
        _state.update {
            it.copy(query = "", committed = "", category = null, filters = ExploreFilters(), sort = LotSort.Ending)
        }
        reload(clear = true)
    }

    /** Opens a shelf's "Ver todos", or a category of Home: the question starts from that and nothing else. */
    fun openWith(query: ShelfQuery) {
        searchJob?.cancel()
        _state.update {
            it.copy(
                query = "",
                committed = "",
                category = query.category,
                filters = ExploreFilters(
                    modality = query.modality,
                    endsWithin = query.endsWithin,
                    acceptsProposal = query.acceptsProposal,
                ),
                sort = LotSort.of(query.sort),
            )
        }
        reload(clear = true)
    }

    /** Asks the screen to put the keyboard up in the search field, as Home's search button does. */
    fun requestSearchFocus() = _state.update { it.copy(focusRequest = it.focusRequest + 1) }

    /** Home's budget bands: the same sheet filters, opened with a price already set. */
    fun openWithBudget(min: Long?, max: Long?) {
        searchJob?.cancel()
        _state.update {
            it.copy(
                query = "",
                committed = "",
                category = null,
                filters = ExploreFilters(minPrice = min, maxPrice = max),
                sort = LotSort.Ending,
            )
        }
        reload(clear = true)
    }

    // ---- the answer ----

    fun refresh() = reload(clear = false, manual = true)

    fun retry() = reload(clear = true)

    fun loadMore() {
        val current = _state.value
        if (current.loading || current.loadingMore || current.pulling || !current.hasNext || current.moreFailed) return
        fetchMore(current)
    }

    fun retryMore() {
        val current = _state.value
        if (current.loadingMore || !current.hasNext) return
        _state.update { it.copy(moreFailed = false) }
        fetchMore(_state.value)
    }

    /** How many lots would match with [filters], for the sheet's button: the same question, one row asked for. */
    suspend fun countFor(filters: ExploreFilters): Long? =
        attempt { repository.lots(query(_state.value, filters = filters, page = 0, size = 1)).totalElements }.getOrNull()

    private fun commit(text: String) {
        _state.update { it.copy(committed = text) }
        reload(clear = true)
    }

    private fun remember(text: String) {
        if (text.isBlank()) return
        _state.update { it.copy(recents = recentSearches.add(text)) }
    }

    private fun loadCategories() {
        if (_state.value.categories.isNotEmpty()) return
        viewModelScope.launch {
            attempt { repository.categories() }
                .onSuccess { categories -> _state.update { it.copy(categories = categories) } }
        }
    }

    private fun reload(clear: Boolean, manual: Boolean = false) {
        loadJob?.cancel()
        moreJob?.cancel()
        _state.update {
            it.copy(
                items = if (clear) emptyList() else it.items,
                total = if (clear) null else it.total,
                loading = clear,
                loadingMore = false,
                pulling = manual,
                error = null,
                moreFailed = false,
                page = 0,
                hasNext = if (clear) false else it.hasNext,
                resetToken = if (clear) it.resetToken + 1 else it.resetToken,
            )
        }
        loadCategories()
        val asked = _state.value
        loadJob = viewModelScope.launch {
            attempt { fetch(asked, page = 0) }
                .onSuccess { result ->
                    _state.update {
                        it.copy(
                            items = result.items,
                            total = result.total,
                            page = result.page,
                            hasNext = result.hasNext,
                            loading = false,
                            pulling = false,
                        )
                    }
                }
                .onFailure { failure ->
                    _state.update { it.copy(loading = false, pulling = false, error = failure.userMessage()) }
                }
        }
    }

    private fun fetchMore(from: ExploreUiState) {
        _state.update { it.copy(loadingMore = true) }
        moreJob = viewModelScope.launch {
            attempt { fetch(from, page = from.page + 1) }
                .onSuccess { result ->
                    _state.update { current ->
                        // The catalogue moves between two pages: a lot can be pushed onto
                        // the next one and arrive twice, and a grid with a repeated key crashes.
                        val seen = current.items.mapTo(HashSet()) { it.id }
                        current.copy(
                            items = current.items + result.items.filter { it.id !in seen },
                            total = result.total,
                            page = result.page,
                            hasNext = result.hasNext,
                            loadingMore = false,
                        )
                    }
                }
                .onFailure { _state.update { it.copy(loadingMore = false, moreFailed = true) } }
        }
    }

    private class Page(val items: List<LotCardModel>, val total: Long, val page: Int, val hasNext: Boolean)

    private suspend fun fetch(asked: ExploreUiState, page: Int): Page {
        val response = repository.lots(query(asked, asked.filters, page, size = PAGE_SIZE))
        val models = withContext(Dispatchers.Default) { response.items.map { it.toCardModel() } }
        return Page(models, response.totalElements, response.page, response.hasNext)
    }

    private fun query(state: ExploreUiState, filters: ExploreFilters, page: Int, size: Int) = LotQuery(
        search = state.committed,
        category = state.category,
        modality = filters.modality,
        endsWithin = filters.endsWithin,
        acceptsProposal = filters.acceptsProposal,
        minPrice = filters.minPrice,
        maxPrice = filters.maxPrice,
        sort = state.sort,
        page = page,
        size = size,
    )

    class Factory(
        private val repository: LoyfusRepository,
        private val recentSearches: RecentSearchesStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ExploreViewModel(repository, recentSearches) as T
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val SEARCH_PAUSE_MILLIS = 320L
    }
}
