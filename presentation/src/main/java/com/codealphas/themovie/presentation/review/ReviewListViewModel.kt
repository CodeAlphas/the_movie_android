package com.codealphas.themovie.presentation.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.auth.LogoutUseCase
import com.codealphas.themovie.domain.result.onFailure
import com.codealphas.themovie.domain.review.Review
import com.codealphas.themovie.domain.review.ReviewRepository
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

data class ReviewListUiState(
    val reviews: List<Review>? = null,
    // ViewModel이 만들어질 때 동기화를 항상 시작하므로, 첫 상태부터 빈 목록 안내 대신 로딩이 보이도록 초깃값을 true로 설정
    val isSyncing: Boolean = true,
)

@HiltViewModel
class ReviewListViewModel
    @Inject
    constructor(
        private val repository: ReviewRepository,
        private val logoutUseCase: LogoutUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ReviewListUiState())
        val uiState: StateFlow<ReviewListUiState> = _uiState.asStateFlow()

        private val _syncFailed = Channel<Unit>(Channel.BUFFERED)
        val syncFailed: Flow<Unit> = _syncFailed.receiveAsFlow()

        private val _logoutCompleted = Channel<Unit>(Channel.BUFFERED)
        val logoutCompleted: Flow<Unit> = _logoutCompleted.receiveAsFlow()

        init {
            viewModelScope.launch {
                // 상태를 통째로 바꾸면 동기화 중에 Room 목록이 올 때 isSyncing이 초깃값으로 돌아가므로, 목록 필드만 갱신
                repository.observeAll().collect { reviews -> _uiState.update { it.copy(reviews = reviews) } }
            }
            // 화면을 열 때마다 동기화하면 작성 화면에서 돌아올 때 서버 값을 다시 읽으므로, ViewModel이 만들어질 때 한 번만 동기화
            viewModelScope.launch {
                val result = repository.syncFromRemote()
                _uiState.update { it.copy(isSyncing = false) }
                result.onFailure { _syncFailed.send(Unit) }
            }
        }

        fun deleteReview(review: Review) {
            viewModelScope.launch {
                repository.delete(review)
            }
        }

        fun logout() {
            viewModelScope.launch {
                logoutUseCase()
                // 삭제가 끝나기 전에 화면을 닫으면 viewModelScope가 취소되어 감상문이 남으므로, 삭제를 마친 뒤 화면 이동 이벤트 전송
                _logoutCompleted.send(Unit)
            }
        }
    }
