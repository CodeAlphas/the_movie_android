package com.codealphas.themovie.presentation.movie

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.movie.MovieDetail
import com.codealphas.themovie.domain.movie.MovieRepository
import com.codealphas.themovie.domain.result.RemoteError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MovieDetailUiState(
    val detail: MovieDetail? = null,
    val isLoading: Boolean = true,
)

@HiltViewModel
class MovieDetailViewModel
    @Inject
    constructor(
        private val repository: MovieRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val movieId: Int = checkNotNull(savedStateHandle.get<Int>(ARG_MOVIE_ID))

        private val _uiState = MutableStateFlow(MovieDetailUiState())
        val uiState: StateFlow<MovieDetailUiState> = _uiState.asStateFlow()

        private val _remoteError = Channel<RemoteError>(Channel.BUFFERED)
        val remoteError: Flow<RemoteError> = _remoteError.receiveAsFlow()

        init {
            // 회전하면 Activity onCreate가 다시 실행되어 요청이 한 번 더 나가므로, ViewModel을 만들 때 한 번만 요청
            loadDetail()
        }

        private fun loadDetail() {
            viewModelScope.launch {
                val result = repository.getMovieDetail(movieId)
                _uiState.value = MovieDetailUiState(detail = result.detail, isLoading = false)
                result.errors.forEach { error -> _remoteError.send(error) }
            }
        }

        companion object {
            const val ARG_MOVIE_ID = "movieId"
        }
    }
