package com.codealphas.themovie.movie

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.movie.Movie
import com.codealphas.themovie.domain.movie.MovieRepository
import com.codealphas.themovie.domain.result.Outcome
import com.codealphas.themovie.domain.result.RemoteError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchMovieUiState(
    val movies: List<Movie>? = null,
)

@HiltViewModel
class SearchMovieViewModel
    @Inject
    constructor(
        private val repository: MovieRepository,
    ) : ViewModel() {
        private val queryText = MutableStateFlow("")

        private val _uiState = MutableStateFlow(SearchMovieUiState())
        val uiState: StateFlow<SearchMovieUiState> = _uiState.asStateFlow()

        private val _remoteError = Channel<RemoteError>(Channel.BUFFERED)
        val remoteError: Flow<RemoteError> = _remoteError.receiveAsFlow()

        init {
            observeQuery()
        }

        // kotlinx-coroutines 1.10.2의 debounce는 FlowPreview이고 mapLatest는 ExperimentalCoroutinesApi라 경고가 나므로,
        // observeQuery에 OptIn 적용
        @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
        private fun observeQuery() {
            // 글자마다 요청하면 앞 검색어 응답이 늦게 도착할 경우 목록을 덮으므로, 입력이 멈춘 뒤의 검색어만 요청하고 이전 요청은 취소
            viewModelScope.launch {
                queryText
                    .debounce(SEARCH_DEBOUNCE_MS)
                    .map(String::trim)
                    .distinctUntilChanged()
                    .filter(String::isNotBlank)
                    .mapLatest { repository.searchMovies(it) }
                    .collect { result ->
                        when (result) {
                            is Outcome.Success -> _uiState.value = SearchMovieUiState(movies = result.data)
                            is Outcome.Failure -> _remoteError.send(result.error)
                        }
                    }
            }
        }

        fun onQueryChange(query: String) {
            queryText.value = query
        }

        private companion object {
            const val SEARCH_DEBOUNCE_MS = 300L
        }
    }
