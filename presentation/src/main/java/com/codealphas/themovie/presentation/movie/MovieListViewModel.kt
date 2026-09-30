package com.codealphas.themovie.presentation.movie

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.movie.Movie
import com.codealphas.themovie.domain.movie.MovieRepository
import com.codealphas.themovie.domain.result.RemoteError
import com.codealphas.themovie.domain.result.onFailure
import com.codealphas.themovie.domain.result.onSuccess
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
import javax.inject.Inject

data class MovieListUiState(
    val movies: List<Movie>? = null,
    val isLoading: Boolean = false,
    val loadError: RemoteError? = null,
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
                    _uiState.update { it.copy(isLoading = true, loadError = null) }
                    val result =
                        when (category) {
                            MovieCategory.POPULAR -> repository.getPopularMovies()
                            MovieCategory.TOP_RATED -> repository.getTopRatedMovies()
                        }
                    result
                        .onSuccess { movies -> _uiState.value = MovieListUiState(movies = movies) }
                        .onFailure { error ->
                            // 실패를 빈 목록으로 넣으면 movies가 null이 아니어서,
                            // 탭이 다시 보일 때 재요청이 멈추므로 목록은 null로 두고 오류만 반영
                            _uiState.update { it.copy(isLoading = false, loadError = error) }
                            _remoteError.send(error)
                        }
                }
        }

        companion object {
            const val ARG_CATEGORY = "movieCategory"
        }
    }
