package com.codealphas.themovie.presentation.notification

import android.os.Build

data class NotificationPermissionState(
    val sdkInt: Int,
    val isGranted: Boolean,
    val shouldShowRationale: Boolean,
) {
    // POST_NOTIFICATIONS는 Android 13(API 33)부터 런타임 권한이므로, 그 전 기기에서 권한 요청 제외
    val requiresRuntimePermission: Boolean
        get() = sdkInt >= Build.VERSION_CODES.TIRAMISU
}

enum class NotificationPromptAction {
    SHOW_PROMPT,
    OPEN_SETTINGS,
    NONE,
}

object NotificationPromptDecision {
    fun onMainEntered(
        state: NotificationPermissionState,
        wasPromptShown: Boolean,
    ): NotificationPromptAction =
        when {
            !state.requiresRuntimePermission || state.isGranted -> NotificationPromptAction.NONE
            // 이 조건이 없으면 나중에를 누른 사용자도 앱을 열 때마다 안내를 보게 되므로, 자동 안내는 한 번만 적용
            wasPromptShown -> NotificationPromptAction.NONE
            else -> NotificationPromptAction.SHOW_PROMPT
        }

    fun onSettingsMenuClicked(
        state: NotificationPermissionState,
        wasPermissionRequested: Boolean,
    ): NotificationPromptAction =
        when {
            // 권한 요청이 없는 기기나 이미 허용한 사용자는 알림을 끄거나 채널을 바꾸려는 것이므로, 앱 알림 설정으로 이동
            !state.requiresRuntimePermission || state.isGranted -> NotificationPromptAction.OPEN_SETTINGS
            // rationale은 한 번도 묻지 않았을 때도 false이므로, 시스템 창을 띄운 적이 없으면 안내 후 요청
            state.shouldShowRationale || !wasPermissionRequested -> NotificationPromptAction.SHOW_PROMPT
            // 다시 묻지 않음이나 두 번 거부 뒤에는 시스템 창이 뜨지 않으므로, 앱 알림 설정에서 켜도록 이동
            else -> NotificationPromptAction.OPEN_SETTINGS
        }
}
