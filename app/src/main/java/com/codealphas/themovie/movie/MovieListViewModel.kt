package com.codealphas.themovie.movie

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.movie.Movie
import com.codealphas.themovie.domain.movie.MovieRepository
import com.codealphas.themovie.domain.result.Outcome
import com.codealphas.themovie.domain.result.RemoteError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MovieListUiState(
    val movies: List<Movie>? = null,
)

@HiltViewModel
class MovieListViewModel
    @Inject
    constructor(
        private val repository: MovieRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val category: MovieCategory = checkNotNull(savedStateHandle.get<MovieCategory>(ARG_CATEGORY))

        private val _uiState = MutableStateFlow(MovieListUiState())
        val uiState: StateFlow<MovieListUiState> = _uiState.asStateFlow()

        private val _remoteError = Channel<RemoteError>(Channel.BUFFERED)
        val remoteError: Flow<RemoteError> = _remoteError.receiveAsFlow()

        private var loadJob: Job? = null

        fun loadMovies() {
            // 응답 전에 다시 호출되면 목록이 아직 null이라 같은 요청이 한 번 더 나가므로, 진행 중인 요청이 있으면 그 응답을 대기
            if (_uiState.value.movies != null || loadJob?.isActive == true) return
            loadJob =
                viewModelScope.launch {
                    val result =
                        when (category) {
                            MovieCategory.POPULAR -> repository.getPopularMovies()
                            MovieCategory.TOP_RATED -> repository.getTopRatedMovies()
                        }
                    when (result) {
                        is Outcome.Success -> _uiState.value = MovieListUiState(movies = result.data)
                        // 실패를 빈 목록으로 넣으면 movies가 null이 아니어서,
                        // 탭이 다시 보일 때 재요청이 멈추므로 오류만 전달
                        is Outcome.Failure -> _remoteError.send(result.error)
                    }
                }
        }

        companion object {
            const val ARG_CATEGORY = "movieCategory"
        }
    }
