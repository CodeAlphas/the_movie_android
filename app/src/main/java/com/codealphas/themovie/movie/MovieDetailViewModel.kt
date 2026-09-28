package com.codealphas.themovie.movie

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.movie.Cast
import com.codealphas.themovie.domain.movie.MovieRepository
import com.codealphas.themovie.domain.movie.Video
import com.codealphas.themovie.domain.result.DataResult
import com.codealphas.themovie.domain.result.RemoteError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MovieDetailUiState(
    val cast: List<Cast>? = null,
    val videos: List<Video>? = null,
)

@HiltViewModel
class MovieDetailViewModel
    @Inject
    constructor(
        private val repository: MovieRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(MovieDetailUiState())
        val uiState: StateFlow<MovieDetailUiState> = _uiState.asStateFlow()

        // replay가 1이면 화면이 다시 구독할 때 마지막 오류를 또 받으므로, 같은 안내가 다시 뜨지 않도록 replay를 0으로 설정
        private val _remoteError = MutableSharedFlow<RemoteError>(replay = 0)
        val remoteError: SharedFlow<RemoteError> = _remoteError

        fun loadCast(movieId: Int) {
            viewModelScope.launch {
                when (val result = repository.getCast(movieId)) {
                    is DataResult.Success -> _uiState.update { it.copy(cast = result.data) }
                    is DataResult.Failure -> _remoteError.emit(result.error)
                }
            }
        }

        fun loadVideos(movieId: Int) {
            viewModelScope.launch {
                when (val result = repository.getVideos(movieId)) {
                    is DataResult.Success -> _uiState.update { it.copy(videos = result.data) }
                    is DataResult.Failure -> _remoteError.emit(result.error)
                }
            }
        }
    }
