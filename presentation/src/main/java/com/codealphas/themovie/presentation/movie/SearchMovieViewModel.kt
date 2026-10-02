package com.codealphas.themovie.presentation.movie

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.movie.MovieRepository
import com.codealphas.themovie.domain.result.onFailure
import com.codealphas.themovie.domain.result.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchMovieViewModel
    @Inject
    constructor(
        private val repository: MovieRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(SearchMovieUiState())
        val state: StateFlow<SearchMovieUiState> = _state.asStateFlow()

        private val _effect = Channel<SearchMovieEffect>(Channel.BUFFERED)
        val effect: Flow<SearchMovieEffect> = _effect.receiveAsFlow()

        private val retryCount = MutableStateFlow(0)

        init {
            observeQuery()
        }

        fun onIntent(intent: SearchMovieIntent) {
            when (intent) {
                is SearchMovieIntent.QueryChanged -> _state.update { it.copy(query = intent.query) }
                SearchMovieIntent.RetryClicked -> retry()
            }
        }

        private fun retry() {
            val current = _state.value
            // 검색어를 모두 지운 뒤 다시 시도하면 검색어 흐름에 남은 앞 검색어로 검색해 빈 입력창에 그 결과가 뜨므로, 빈 검색어면 다시 시도 제외
            if (current.loadError == null || current.query.isBlank()) return
            retryCount.update { it + 1 }
        }

        // kotlinx-coroutines 1.10.2의 debounce는 FlowPreview이고 mapLatest는 ExperimentalCoroutinesApi라 경고가 나므로,
        // observeQuery에 OptIn 적용
        @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
        private fun observeQuery() {
            // 글자마다 요청하면 앞 검색어 응답이 늦게 도착할 경우 목록을 덮으므로, 입력이 멈춘 뒤의 검색어만 요청하고 이전 요청은 취소
            viewModelScope.launch {
                _state
                    .map { it.query }
                    // 로딩과 결과 반영도 state를 바꿔 같은 검색어가 다시 오면 debounce가 처음부터 다시 기다리므로, 검색어가 바뀔 때만 전달
                    .distinctUntilChanged()
                    .debounce(SEARCH_DEBOUNCE_MS)
                    .map(String::trim)
                    .distinctUntilChanged()
                    .filter(String::isNotBlank)
                    // 앞 검색어와 같은 검색어는 위 distinctUntilChanged에서 걸러져 실패한 검색어를 다시 검색할 수 없으므로,
                    // 다시 시도 횟수가 오를 때마다 마지막 검색어를 mapLatest에 한 번 더 전달
                    .combine(retryCount) { query, _ -> query }
                    .mapLatest { query ->
                        _state.update { it.copy(isLoading = true, loadError = null) }
                        repository.searchMovies(query)
                    }.collect { result ->
                        result
                            .onSuccess { movies ->
                                _state.update { it.copy(movies = movies, isLoading = false, loadError = null) }
                            }.onFailure { error ->
                                _state.update { it.copy(isLoading = false, loadError = error) }
                                _effect.send(SearchMovieEffect.ShowError(error))
                            }
                    }
            }
        }

        private companion object {
            const val SEARCH_DEBOUNCE_MS = 300L
        }
    }
