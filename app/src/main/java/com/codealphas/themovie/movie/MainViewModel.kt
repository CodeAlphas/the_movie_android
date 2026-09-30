package com.codealphas.themovie.movie

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.auth.AuthRepository
import com.codealphas.themovie.domain.auth.LogoutUseCase
import com.codealphas.themovie.domain.notification.NotificationPromptRepository
import com.codealphas.themovie.notification.NotificationPermissionState
import com.codealphas.themovie.notification.NotificationPromptAction
import com.codealphas.themovie.notification.NotificationPromptDecision
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val logoutUseCase: LogoutUseCase,
        private val notificationPromptRepository: NotificationPromptRepository,
    ) : ViewModel() {
        private val _logoutCompleted = Channel<Unit>(Channel.BUFFERED)
        val logoutCompleted: Flow<Unit> = _logoutCompleted.receiveAsFlow()

        private val _notificationPrompt = Channel<NotificationPromptAction>(Channel.BUFFERED)
        val notificationPrompt: Flow<NotificationPromptAction> = _notificationPrompt.receiveAsFlow()

        fun isSignedIn(): Boolean = authRepository.currentUserId() != null

        fun logout() {
            viewModelScope.launch {
                logoutUseCase()
                // 삭제가 끝나기 전에 화면을 닫으면 viewModelScope가 취소되어 감상문이 남으므로, 삭제를 마친 뒤 화면 이동 이벤트 전송
                _logoutCompleted.send(Unit)
            }
        }

        fun onMainEntered(state: NotificationPermissionState) {
            viewModelScope.launch {
                val wasPromptShown = notificationPromptRepository.wasPromptShown()
                sendNotificationPrompt(NotificationPromptDecision.onMainEntered(state, wasPromptShown))
            }
        }

        fun onNotificationSettingsClicked(state: NotificationPermissionState) {
            viewModelScope.launch {
                val wasPermissionRequested = notificationPromptRepository.wasPermissionRequested()
                sendNotificationPrompt(NotificationPromptDecision.onSettingsMenuClicked(state, wasPermissionRequested))
            }
        }

        fun onNotificationPromptAccepted() {
            viewModelScope.launch {
                notificationPromptRepository.markPromptShown()
                notificationPromptRepository.markPermissionRequested()
            }
        }

        fun onNotificationPromptDeclined() {
            viewModelScope.launch { notificationPromptRepository.markPromptShown() }
        }

        private suspend fun sendNotificationPrompt(action: NotificationPromptAction) {
            if (action != NotificationPromptAction.NONE) _notificationPrompt.send(action)
        }
    }
