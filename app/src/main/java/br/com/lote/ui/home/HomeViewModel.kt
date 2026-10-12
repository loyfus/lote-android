package br.com.lote.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.lote.data.Category
import br.com.lote.data.ClosingWindow
import br.com.lote.data.LoteRepository
import br.com.lote.data.ShelfQuery
import br.com.lote.data.attempt
import br.com.lote.data.userMessage
import br.com.lote.ui.components.LotCardModel
import br.com.lote.ui.components.toCardModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A shelf of Home, with its lots already prepared for display. */
class ShelfModel(
    val id: String,
    val title: String,
    val subtitle: String?,
    val query: ShelfQuery,
    val items: List<LotCardModel>,
)

data class HomeUiState(
    val loaded: Boolean = false,
    val shelves: List<ShelfModel> = emptyList(),
    val categories: List<Category> = emptyList(),
    /** When the server last assembled the page: the freshness label reads this. */
    val updatedAt: String? = null,
    /** How many lots the catalogue holds, for the context line under the hero. */
    val totalLots: Long = 0,
    /** The nearest deadline window that still has auctions, or null when none closes in a week. */
    val closing: ClosingWindow? = null,
    /** How many lots take an offer below the asking price. */
    val acceptsProposal: Long = 0,
    val refreshing: Boolean = false,
    val pulling: Boolean = false,
    val error: String? = null,
)

/**
 * Home is discovery, and the server does the choosing: the shelves arrive assembled, so this
 * holds no feed, no paging and no ranking of its own. What it adds is the category row and the
 * freshness of both.
 */
class HomeViewModel(private val repository: LoteRepository) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var job: Job? = null

    init {
        load(manual = false)
    }

    fun load(manual: Boolean) {
        if (job?.isActive == true) {
            if (manual) _state.update { it.copy(pulling = true) }
            return
        }
        _state.update { it.copy(refreshing = true, pulling = manual, error = null) }
        job = viewModelScope.launch {
            attempt {
                coroutineScope {
                    val home = async { repository.home() }
                    // The categories are an extra: a failure there leaves the row out
                    // and keeps the shelves, instead of failing the whole screen.
                    val categories = async { attempt { repository.categories() }.getOrNull() }
                    val response = home.await()
                    val shelves = withContext(Dispatchers.Default) {
                        response.shelves.map { shelf ->
                            ShelfModel(
                                id = shelf.id,
                                title = shelf.title,
                                subtitle = shelf.subtitle,
                                query = shelf.query,
                                items = shelf.items.map { it.toCardModel() },
                            )
                        }
                    }
                    HomePayload(shelves, categories.await(), response.updatedAt, response.totalLots, response.closing, response.acceptsProposal)
                }
            }.onSuccess { payload ->
                _state.update {
                    it.copy(
                        loaded = true,
                        shelves = payload.shelves,
                        categories = payload.categories ?: it.categories,
                        updatedAt = payload.updatedAt,
                        totalLots = payload.totalLots,
                        closing = payload.closing,
                        acceptsProposal = payload.acceptsProposal,
                        refreshing = false,
                        pulling = false,
                    )
                }
            }.onFailure { failure ->
                _state.update { it.copy(refreshing = false, pulling = false, error = failure.userMessage()) }
            }
        }
    }

    class Factory(private val repository: LoteRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(repository) as T
    }
}

/** One successful read of the whole page, so the update below is a single atomic swap. */
private data class HomePayload(
    val shelves: List<ShelfModel>,
    val categories: List<Category>?,
    val updatedAt: String?,
    val totalLots: Long,
    val closing: ClosingWindow?,
    val acceptsProposal: Long,
)
