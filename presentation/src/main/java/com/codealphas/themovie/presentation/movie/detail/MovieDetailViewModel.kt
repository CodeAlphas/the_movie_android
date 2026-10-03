package com.codealphas.themovie.presentation.movie.detail

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
import kotlinx.coroutines.flow.update
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

        fun onIntent(intent: MovieDetailIntent) {
            when (intent) {
                MovieDetailIntent.RetryClicked -> retry()
            }
        }

        private fun retry() {
            if (_state.value.isLoading) return
            loadDetail()
        }

        private fun loadDetail() {
            // 코루틴이 시작되기 전에 다시 시도를 한 번 더 누르면 같은 요청이 두 번 나가므로, 요청 중 상태는 launch 전에 반영
            _state.update { it.copy(isLoading = true, loadError = null) }
            viewModelScope.launch {
                val result = repository.getMovieDetail(movieId)
                // 본문이 실패하면 앱바 아래가 빈 채로 남으므로, 다시 시도 버튼을 보이도록 본문 오류를 loadError에 반영
                val loadError = if (result.detail == null) result.errors.firstOrNull() else null
                _state.value = MovieDetailUiState(detail = result.detail, isLoading = false, loadError = loadError)
                // 출연진과 영상이 함께 실패하면 같은 화면에서 안내가 연달아 뜨므로, 첫 오류만 알리도록 처리
                result.errors.firstOrNull()?.let { error -> _effect.send(MovieDetailEffect.ShowError(error)) }
            }
        }
    }
