package br.com.loyfus.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.loyfus.data.ApiException
import br.com.loyfus.data.LoyfusRepository
import br.com.loyfus.data.attempt
import br.com.loyfus.data.userMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DetailUiState(
    val loading: Boolean = true,
    val model: DetailModel? = null,
    /** A dropped connection and a deleted lot both end without a lot, and need different words. */
    val notFound: Boolean = false,
    val error: String? = null,
)

class DetailViewModel(
    private val repository: LoyfusRepository,
    private val lotId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { DetailUiState(loading = true) }
        viewModelScope.launch {
            attempt {
                val lot = repository.lot(lotId)
                withContext(Dispatchers.Default) { lot.toDetailModel() }
            }.onSuccess { model ->
                _state.value = DetailUiState(loading = false, model = model)
            }.onFailure { failure ->
                _state.value = DetailUiState(
                    loading = false,
                    notFound = failure is ApiException && failure.code == 404,
                    error = failure.userMessage(),
                )
            }
        }
    }

    class Factory(
        private val repository: LoyfusRepository,
        private val lotId: Long,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = DetailViewModel(repository, lotId) as T
    }
}
