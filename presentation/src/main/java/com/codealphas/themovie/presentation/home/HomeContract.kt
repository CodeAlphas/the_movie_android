package com.codealphas.themovie.presentation.home

import com.codealphas.themovie.presentation.notification.NotificationPermissionState

data class HomeUiState(
    val showNotificationPrompt: Boolean = false,
    // 로그아웃 확인 창은 서버에 반영하지 않은 감상문 변경이 있을 때만 띄우므로, 창에 보일 변경 수를 담고 창을 띄우지 않을 때는 null 적용
    val logoutPendingReviewCount: Int? = null,
)

sealed interface HomeIntent {
    data class Entered(
        val permissionState: NotificationPermissionState,
    ) : HomeIntent

    data object LogoutClicked : HomeIntent

    data object LogoutConfirmed : HomeIntent

    data object LogoutDismissed : HomeIntent

    data class NotificationSettingsClicked(
        val permissionState: NotificationPermissionState,
    ) : HomeIntent

    data object NotificationPromptAccepted : HomeIntent

    data object NotificationPromptDeclined : HomeIntent
}

sealed interface HomeEffect {
    data object NavigateToLogin : HomeEffect

    data object RequestNotificationPermission : HomeEffect

    data object OpenNotificationSettings : HomeEffect
}
