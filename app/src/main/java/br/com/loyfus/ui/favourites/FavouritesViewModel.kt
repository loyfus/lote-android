package br.com.loyfus.ui.favourites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.loyfus.data.LoyfusRepository
import br.com.loyfus.data.attempt
import br.com.loyfus.data.userMessage
import br.com.loyfus.ui.components.LotCardModel
import br.com.loyfus.ui.components.toCardModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FavouritesUiState(
    /** Every saved lot of this device: the hearts on every screen and the badge on the tab read this. */
    val ids: Set<Long> = emptySet(),
    /**
     * The Saved list as of the last read of the server. A removed row stays here until the next
     * read and is hidden by [ids], so a refused removal brings it back without another round trip.
     */
    val items: List<LotCardModel> = emptyList(),
    /** True once the server has answered at least once. */
    val loaded: Boolean = false,
    /** A request is in flight, started by the app. */
    val refreshing: Boolean = false,
    /** A request is in flight, started by the reader pulling the list down. */
    val pulling: Boolean = false,
    val error: String? = null,
)

/**
 * The saved lots of this device, in one place for the whole app. Held above the navigation graph,
 * like Explore, because three screens and the tab bar need the same answer: a heart tapped in
 * Explore must already be filled in Home and counted on the Saved tab.
 *
 * Every change is optimistic and honest: the heart answers at once, the request follows, and if the
 * server refuses, the heart goes back.
 */
class FavouritesViewModel(private val repository: LoyfusRepository) : ViewModel() {

    private val _state = MutableStateFlow(FavouritesUiState())
    val state: StateFlow<FavouritesUiState> = _state.asStateFlow()

    // A heart the server refused, after it has gone back: `true` when saving failed,
    // `false` when removing did. The screen says so; a heart that silently changes its
    // mind tells the reader nothing about why.
    private val _refusals = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val refusals: SharedFlow<Boolean> = _refusals.asSharedFlow()

    // Changes the server has not confirmed yet. A read that lands while a heart is in
    // flight must not undo it.
    private val pending = HashMap<Long, Boolean>()
    private var refreshJob: Job? = null

    init {
        refresh(manual = false)
    }

    /** Reads the list from the server. A read already in flight is not repeated. */
    fun refresh(manual: Boolean) {
        if (refreshJob?.isActive == true) {
            if (manual) _state.update { it.copy(pulling = true) }
            return
        }
        _state.update { it.copy(refreshing = true, pulling = manual, error = null) }
        refreshJob = viewModelScope.launch {
            attempt {
                val lots = repository.favourites()
                withContext(Dispatchers.Default) { lots.map { it.toCardModel() } }
            }.onSuccess { models ->
                _state.update { current ->
                    val ids = models.map { it.id }.toSet().applying(pending)
                    current.copy(ids = ids, items = models, loaded = true, refreshing = false, pulling = false)
                }
            }.onFailure { failure ->
                _state.update { it.copy(refreshing = false, pulling = false, error = failure.userMessage()) }
            }
        }
    }

    /** Sets a lot's heart to [on], the value the reader sees after the tap. */
    fun set(lotId: Long, on: Boolean) {
        _state.update { it.copy(ids = if (on) it.ids + lotId else it.ids - lotId) }
        send(lotId, on)
    }

    /** The undo of a removal: the row comes back, even if a read pruned it meanwhile, and the server is told again. */
    fun restore(item: LotCardModel) {
        _state.update { current ->
            current.copy(
                ids = current.ids + item.id,
                items = if (current.items.any { it.id == item.id }) current.items else current.items + item,
            )
        }
        send(item.id, true)
    }

    private fun send(lotId: Long, on: Boolean) {
        pending[lotId] = on
        viewModelScope.launch {
            val result = attempt {
                if (on) repository.addFavourite(lotId) else repository.removeFavourite(lotId)
            }
            // A newer tap on the same heart owns the pending entry now; leave it alone.
            if (pending[lotId] == on) pending.remove(lotId)
            if (result.isFailure && pending[lotId] == null) {
                _state.update { it.copy(ids = if (on) it.ids - lotId else it.ids + lotId) }
                _refusals.tryEmit(on)
            }
        }
    }

    private fun Set<Long>.applying(changes: Map<Long, Boolean>): Set<Long> {
        var result = this
        for ((id, on) in changes) result = if (on) result + id else result - id
        return result
    }

    class Factory(private val repository: LoyfusRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = FavouritesViewModel(repository) as T
    }
}
