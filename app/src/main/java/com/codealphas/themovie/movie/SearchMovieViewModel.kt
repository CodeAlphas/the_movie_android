package com.codealphas.themovie.movie

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.movie.Movie
import com.codealphas.themovie.domain.movie.MovieRepository
import com.codealphas.themovie.domain.result.DataResult
import com.codealphas.themovie.domain.result.RemoteError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
        private val _uiState = MutableStateFlow(SearchMovieUiState())
        val uiState: StateFlow<SearchMovieUiState> = _uiState.asStateFlow()

        // replay가 1이면 화면이 다시 구독할 때 마지막 오류를 또 받으므로, 같은 안내가 다시 뜨지 않도록 replay를 0으로 설정
        private val _remoteError = MutableSharedFlow<RemoteError>(replay = 0)
        val remoteError: SharedFlow<RemoteError> = _remoteError

        fun search(query: String) {
            viewModelScope.launch {
                when (val result = repository.searchMovies(query)) {
                    is DataResult.Success -> _uiState.value = SearchMovieUiState(movies = result.data)
                    is DataResult.Failure -> _remoteError.emit(result.error)
                }
            }
        }
    }
