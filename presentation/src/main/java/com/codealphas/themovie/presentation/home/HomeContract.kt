package com.codealphas.themovie.presentation.home

import com.codealphas.themovie.presentation.notification.NotificationPermissionState

data class HomeUiState(
    val showNotificationPrompt: Boolean = false,
)

sealed interface HomeIntent {
    data class Entered(
        val permissionState: NotificationPermissionState,
    ) : HomeIntent

    data object LogoutClicked : HomeIntent

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
