package com.codealphas.themovie.presentation.movie

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.movie.MovieRepository
import com.codealphas.themovie.presentation.navigation.MovieDetail
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
        // 라우트 인자를 꺼내는 기본 방법인 toRoute()는 Bundle이 필요해 JVM 단위 테스트에서 실패하므로,
        // 테스트에서도 읽히도록 Navigation이 속성 이름을 키로 넣어 둔 값 직접 조회
        private val movieId: Int = checkNotNull(savedStateHandle.get<Int>(MovieDetail::movieId.name))

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
    }
