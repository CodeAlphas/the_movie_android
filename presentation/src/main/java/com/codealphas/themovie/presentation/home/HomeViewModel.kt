package com.codealphas.themovie.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.auth.LogoutUseCase
import com.codealphas.themovie.domain.notification.NotificationPermissionRepository
import com.codealphas.themovie.domain.review.ReviewRepository
import com.codealphas.themovie.presentation.notification.NotificationPermissionAction
import com.codealphas.themovie.presentation.notification.NotificationPermissionDecision
import com.codealphas.themovie.presentation.notification.NotificationPermissionState
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

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val logoutUseCase: LogoutUseCase,
        private val notificationPermissionRepository: NotificationPermissionRepository,
        private val reviewRepository: ReviewRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(HomeUiState())
        val state: StateFlow<HomeUiState> = _state.asStateFlow()

        private val _effect = Channel<HomeEffect>(Channel.BUFFERED)
        val effect: Flow<HomeEffect> = _effect.receiveAsFlow()

        private var logoutJob: Job? = null

        fun onIntent(intent: HomeIntent) {
            when (intent) {
                is HomeIntent.Entered -> onHomeEntered(intent.permissionState)
                HomeIntent.LogoutClicked -> onLogoutClicked()
                HomeIntent.LogoutConfirmed -> onLogoutConfirmed()
                HomeIntent.LogoutDismissed -> _state.update { it.copy(logoutPendingReviewCount = null) }
                is HomeIntent.NotificationSettingsClicked -> onNotificationSettingsClicked(intent.permissionState)
                HomeIntent.NotificationPromptAccepted -> onNotificationPromptAccepted()
                HomeIntent.NotificationPromptDeclined -> onNotificationPromptDeclined()
            }
        }

        private fun onLogoutClicked() {
            if (isLogoutRunning()) return
            logoutJob =
                viewModelScope.launch {
                    // 로그아웃하면 Room 감상문을 지워 서버에 반영하지 않은 변경이 사라지므로,
                    // 사용자가 알고 고르도록 바로 로그아웃하지 않고 변경 수와 함께 확인 창 표시
                    val pendingCount = reviewRepository.countPendingChanges()
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
            logoutUseCase()
            // 삭제가 끝나기 전에 화면을 닫으면 viewModelScope가 취소되어 감상문이 남으므로, 삭제를 마친 뒤 화면 이동 이벤트 전송
            _effect.send(HomeEffect.NavigateToLogin)
        }

        private fun onHomeEntered(state: NotificationPermissionState) {
            viewModelScope.launch {
                val wasPromptAnswered = notificationPermissionRepository.wasPromptAnswered()
                applyNotificationPermissionAction(
                    NotificationPermissionDecision.onHomeEntered(state, wasPromptAnswered),
                )
            }
        }

        private fun onNotificationSettingsClicked(state: NotificationPermissionState) {
            viewModelScope.launch {
                val wasPermissionRequested = notificationPermissionRepository.wasPermissionRequested()
                applyNotificationPermissionAction(
                    NotificationPermissionDecision.onSettingsMenuClicked(state, wasPermissionRequested),
                )
            }
        }

        private fun onNotificationPromptAccepted() {
            _state.update { it.copy(showNotificationPrompt = false) }
            viewModelScope.launch {
                _effect.send(HomeEffect.RequestNotificationPermission)
                notificationPermissionRepository.markPromptAnswered()
                notificationPermissionRepository.markPermissionRequested()
            }
        }

        private fun onNotificationPromptDeclined() {
            _state.update { it.copy(showNotificationPrompt = false) }
            viewModelScope.launch { notificationPermissionRepository.markPromptAnswered() }
        }

        private suspend fun applyNotificationPermissionAction(action: NotificationPermissionAction) {
            when (action) {
                NotificationPermissionAction.SHOW_PROMPT -> _state.update { it.copy(showNotificationPrompt = true) }
                NotificationPermissionAction.OPEN_SETTINGS -> _effect.send(HomeEffect.OpenNotificationSettings)
                NotificationPermissionAction.NONE -> Unit
            }
        }
    }
