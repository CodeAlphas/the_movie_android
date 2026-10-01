package com.codealphas.themovie.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.auth.AuthRepository
import com.codealphas.themovie.domain.auth.LogoutUseCase
import com.codealphas.themovie.domain.notification.NotificationPromptRepository
import com.codealphas.themovie.presentation.notification.NotificationPermissionState
import com.codealphas.themovie.presentation.notification.NotificationPromptAction
import com.codealphas.themovie.presentation.notification.NotificationPromptDecision
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
class HomeViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val logoutUseCase: LogoutUseCase,
        private val notificationPromptRepository: NotificationPromptRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(HomeUiState())
        val state: StateFlow<HomeUiState> = _state.asStateFlow()

        private val _effect = Channel<HomeEffect>(Channel.BUFFERED)
        val effect: Flow<HomeEffect> = _effect.receiveAsFlow()

        fun isSignedIn(): Boolean = authRepository.currentUserId() != null

        fun onIntent(intent: HomeIntent) {
            when (intent) {
                is HomeIntent.Entered -> onHomeEntered(intent.permissionState)
                HomeIntent.LogoutClicked -> logout()
                is HomeIntent.NotificationSettingsClicked -> onNotificationSettingsClicked(intent.permissionState)
                HomeIntent.NotificationPromptAccepted -> onNotificationPromptAccepted()
                HomeIntent.NotificationPromptDeclined -> onNotificationPromptDeclined()
            }
        }

        private fun logout() {
            viewModelScope.launch {
                logoutUseCase()
                // 삭제가 끝나기 전에 화면을 닫으면 viewModelScope가 취소되어 감상문이 남으므로, 삭제를 마친 뒤 화면 이동 이벤트 전송
                _effect.send(HomeEffect.NavigateToLogin)
            }
        }

        private fun onHomeEntered(state: NotificationPermissionState) {
            viewModelScope.launch {
                val wasPromptShown = notificationPromptRepository.wasPromptShown()
                applyNotificationPrompt(NotificationPromptDecision.onMainEntered(state, wasPromptShown))
            }
        }

        private fun onNotificationSettingsClicked(state: NotificationPermissionState) {
            viewModelScope.launch {
                val wasPermissionRequested = notificationPromptRepository.wasPermissionRequested()
                applyNotificationPrompt(NotificationPromptDecision.onSettingsMenuClicked(state, wasPermissionRequested))
            }
        }

        private fun onNotificationPromptAccepted() {
            _state.update { it.copy(showNotificationPrompt = false) }
            viewModelScope.launch {
                _effect.send(HomeEffect.RequestNotificationPermission)
                notificationPromptRepository.markPromptShown()
                notificationPromptRepository.markPermissionRequested()
            }
        }

        private fun onNotificationPromptDeclined() {
            _state.update { it.copy(showNotificationPrompt = false) }
            viewModelScope.launch { notificationPromptRepository.markPromptShown() }
        }

        private suspend fun applyNotificationPrompt(action: NotificationPromptAction) {
            when (action) {
                NotificationPromptAction.SHOW_RATIONALE -> _state.update { it.copy(showNotificationPrompt = true) }
                NotificationPromptAction.OPEN_SETTINGS -> _effect.send(HomeEffect.OpenNotificationSettings)
                NotificationPromptAction.NONE -> Unit
            }
        }
    }
