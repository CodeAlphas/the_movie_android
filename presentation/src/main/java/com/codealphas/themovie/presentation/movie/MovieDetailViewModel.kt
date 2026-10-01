package com.codealphas.themovie.presentation.movie

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.movie.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MovieDetailViewModel
    @Inject
    constructor(
        private val repository: MovieRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val movieId: Int = checkNotNull(savedStateHandle.get<Int>(ARG_MOVIE_ID))

        private val _state = MutableStateFlow(MovieDetailUiState())
        val state: StateFlow<MovieDetailUiState> = _state.asStateFlow()

        private val _effect = Channel<MovieDetailEffect>(Channel.BUFFERED)
        val effect: Flow<MovieDetailEffect> = _effect.receiveAsFlow()

        init {
            // 회전할 때마다 같은 상세를 다시 받지 않도록, 회전에도 남는 ViewModel이 만들어질 때 한 번만 요청
            loadDetail()
        }

        private fun loadDetail() {
            viewModelScope.launch {
                val result = repository.getMovieDetail(movieId)
                _state.value = MovieDetailUiState(detail = result.detail, isLoading = false)
                result.errors.forEach { error -> _effect.send(MovieDetailEffect.ShowError(error)) }
            }
        }

        companion object {
            const val ARG_MOVIE_ID = "movieId"
        }
    }
