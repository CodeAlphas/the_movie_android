package com.codealphas.themovie.presentation.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.auth.LogoutUseCase
import com.codealphas.themovie.domain.result.onFailure
import com.codealphas.themovie.domain.result.onSuccess
import com.codealphas.themovie.domain.review.Review
import com.codealphas.themovie.domain.review.ReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
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
class ReviewListViewModel
    @Inject
    constructor(
        private val repository: ReviewRepository,
        private val logoutUseCase: LogoutUseCase,
    ) : ViewModel() {
        private val _state = MutableStateFlow(ReviewListUiState())
        val state: StateFlow<ReviewListUiState> = _state.asStateFlow()

        private val _effect = Channel<ReviewListEffect>(Channel.BUFFERED)
        val effect: Flow<ReviewListEffect> = _effect.receiveAsFlow()

        private val syncJob: Job

        private var logoutJob: Job? = null

        init {
            viewModelScope.launch {
                // 상태를 통째로 바꾸면 동기화 중에 Room 목록이 올 때 isSyncing이 초깃값으로 돌아가므로, 목록 필드만 갱신
                repository.observeAll().collect { reviews -> _state.update { it.copy(reviews = reviews) } }
            }
            // 화면을 열 때마다 동기화하면 작성 화면에서 돌아올 때 서버 값을 다시 읽으므로, ViewModel이 만들어질 때 한 번만 동기화
            syncJob =
                viewModelScope.launch {
                    val result = repository.syncFromRemote()
                    _state.update { it.copy(isSyncing = false) }
                    result.onFailure {
                        // 목록에 Room 감상문이 보이면 서버를 읽지 못해도 사용자가 볼 내용이 있으므로, 보여 줄 감상문이 없을 때만 실패 안내
                        if (_state.value.reviews.isNullOrEmpty()) _effect.send(ReviewListEffect.ShowSyncFailed)
                    }
                }
        }

        fun onIntent(intent: ReviewListIntent) {
            when (intent) {
                is ReviewListIntent.DeleteClicked -> deleteReview(intent.review)
                ReviewListIntent.LogoutClicked -> onLogoutClicked()
                ReviewListIntent.LogoutConfirmed -> onLogoutConfirmed()
                ReviewListIntent.LogoutDismissed -> _state.update { it.copy(logoutPendingReviewCount = null) }
            }
        }

        private fun deleteReview(review: Review) {
            viewModelScope.launch {
                repository
                    .delete(review)
                    .onSuccess { _effect.send(ReviewListEffect.ShowDeleted) }
                    .onFailure { _effect.send(ReviewListEffect.ShowDeleteFailed) }
            }
        }

        private fun onLogoutClicked() {
            if (isLogoutRunning()) return
            logoutJob =
                viewModelScope.launch {
                    // 로그아웃하면 Room 감상문을 지워 서버에 반영하지 않은 변경이 사라지므로,
                    // 사용자가 알고 고르도록 바로 로그아웃하지 않고 변경 수와 함께 확인 창 표시
                    val pendingCount = repository.countPendingChanges()
                    if (pendingCount > 0) {
                        _state.update { it.copy(logoutPendingReviewCount = pendingCount) }
                    } else {
                        logout()
                    }
                }
        }

        private fun onLogoutConfirmed() {
            if (isLogoutRunning()) return
            _state.update { it.copy(logoutPendingReviewCount = null) }
            logoutJob = viewModelScope.launch { logout() }
        }

        // 확인 창의 로그아웃 버튼은 연타를 막지 않아 두 번 누르면 감상문 삭제와 로그인 화면 이동이 두 번 일어나므로,
        // 진행 중인 로그아웃이 끝나기 전에 들어온 로그아웃 요청은 무시
        private fun isLogoutRunning(): Boolean = logoutJob?.isActive == true

        private suspend fun logout() {
            // 동기화가 감상문 삭제 뒤에 끝나면 이전 계정의 감상문이 Room에 다시 들어가 다음 계정의 목록에 보이므로, 동기화를 멈춘 뒤 로그아웃
            // 확인 창을 띄울 때 동기화를 멈추는데 사용자가 취소하면 동기화 중 표시가 풀리지 않으므로, 로그아웃을 실제로 진행할 때만 동기화 중단
            syncJob.cancelAndJoin()
            logoutUseCase()
            // 삭제가 끝나기 전에 화면을 닫으면 viewModelScope가 취소되어 감상문이 남으므로, 삭제를 마친 뒤 화면 이동 이벤트 전송
            _effect.send(ReviewListEffect.NavigateToLogin)
        }
    }
