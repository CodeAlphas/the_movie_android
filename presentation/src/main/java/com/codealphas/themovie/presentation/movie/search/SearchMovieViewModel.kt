package com.codealphas.themovie.presentation.movie.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.codealphas.themovie.domain.movie.Movie
import com.codealphas.themovie.domain.movie.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class SearchMovieViewModel
    @Inject
    constructor(
        repository: MovieRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(SearchMovieUiState())
        val state: StateFlow<SearchMovieUiState> = _state.asStateFlow()

        // kotlinx-coroutines 1.10.2의 debounce는 FlowPreview이고 flatMapLatest는 ExperimentalCoroutinesApi라 경고가 나므로,
        // movies에 OptIn 적용
        @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
        val movies: Flow<PagingData<Movie>> =
            _state
                .map { it.query }
                // 글자마다 요청하면 앞 검색어 응답이 늦게 도착할 경우 목록을 덮으므로, 입력이 멈춘 뒤의 검색어만 요청
                .debounce(SEARCH_DEBOUNCE)
                .map(String::trim)
                // 앞뒤 공백만 다른 검색어로 새 검색을 시작하면 결과가 처음부터 다시 로드되므로, 같은 검색어는 전달 제외
                .distinctUntilChanged()
                // 검색어가 바뀌면 이전 검색어의 Pager를 버리고 새 Pager로 바꾸고, 빈 검색어는 검색 전과 같은 빈 목록 적용
                .flatMapLatest { query ->
                    if (query.isBlank()) flowOf(idleMovies()) else repository.searchMovies(query)
                }
                // 검색어가 오기 전에는 LazyPagingItems가 refresh를 Loading으로 두어 진행 표시가 계속 보일 수 있으므로, 빈 목록을 먼저 전달
                .onStart { emit(idleMovies()) }
                // 화면 회전이나 탭 전환으로 다시 수집해도 검색 결과를 다시 요청하지 않도록, viewModelScope에 보관
                .cachedIn(viewModelScope)

        fun onIntent(intent: SearchMovieIntent) {
            when (intent) {
                is SearchMovieIntent.QueryChanged -> _state.update { it.copy(query = intent.query) }
            }
        }

        // 인자 없는 PagingData.empty()는 로드 상태를 보내지 않아 Loading이 남을 수 있으므로, 세 상태를 NotLoading으로 지정
        private fun idleMovies(): PagingData<Movie> {
            val notLoading = LoadState.NotLoading(endOfPaginationReached = false)
            return PagingData.empty(
                sourceLoadStates = LoadStates(refresh = notLoading, prepend = notLoading, append = notLoading),
            )
        }

        private companion object {
            val SEARCH_DEBOUNCE = 300.milliseconds
        }
    }
