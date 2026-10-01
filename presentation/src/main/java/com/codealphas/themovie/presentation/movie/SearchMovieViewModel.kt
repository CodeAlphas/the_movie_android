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

        init {
            observeQuery()
        }

        fun onIntent(intent: SearchMovieIntent) {
            when (intent) {
                is SearchMovieIntent.QueryChanged -> _state.update { it.copy(query = intent.query) }
            }
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
