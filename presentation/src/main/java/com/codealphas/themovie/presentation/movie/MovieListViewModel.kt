package com.codealphas.themovie.presentation.movie

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.movie.MovieRepository
import com.codealphas.themovie.domain.result.onFailure
import com.codealphas.themovie.domain.result.onSuccess
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 탭 라우트에 인자가 없어 SavedStateHandle로 분류를 받을 수 없으므로, 화면에서 분류를 직접 넘기도록 assisted injection 적용
@HiltViewModel(assistedFactory = MovieListViewModel.Factory::class)
class MovieListViewModel
    @AssistedInject
    constructor(
        private val repository: MovieRepository,
        @Assisted val category: MovieCategory,
    ) : ViewModel() {
        private val _state = MutableStateFlow(MovieListUiState())
        val state: StateFlow<MovieListUiState> = _state.asStateFlow()

        private val _effect = Channel<MovieListEffect>(Channel.BUFFERED)
        val effect: Flow<MovieListEffect> = _effect.receiveAsFlow()

        private var loadJob: Job? = null

        fun onIntent(intent: MovieListIntent) {
            when (intent) {
                MovieListIntent.PageShown,
                MovieListIntent.RetryClicked,
                -> loadMovies()
            }
        }

        private fun loadMovies() {
            // 응답 전에 다시 호출되면 목록이 아직 null이라 같은 요청이 한 번 더 나가므로, 진행 중인 요청이 있으면 새 요청 제외
            if (_state.value.movies != null || loadJob?.isActive == true) return
            loadJob =
                viewModelScope.launch {
                    _state.update { it.copy(isLoading = true, loadError = null) }
                    val result =
                        when (category) {
                            MovieCategory.POPULAR -> repository.getPopularMovies()
                            MovieCategory.TOP_RATED -> repository.getTopRatedMovies()
                        }
                    result
                        .onSuccess { movies -> _state.value = MovieListUiState(movies = movies) }
                        .onFailure { error ->
                            // 실패를 빈 목록으로 넣으면 화면이 다시 보일 때 오는 PageShown에서 목록이 있다고 보고 다시 요청하지 않으므로,
                            // 목록은 null로 두고 오류만 반영
                            _state.update { it.copy(isLoading = false, loadError = error) }
                            _effect.send(MovieListEffect.ShowError(error))
                        }
                }
        }

        @AssistedFactory
        interface Factory {
            fun create(category: MovieCategory): MovieListViewModel
        }
    }
