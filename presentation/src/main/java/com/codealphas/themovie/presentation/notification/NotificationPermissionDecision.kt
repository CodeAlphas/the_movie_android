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

enum class NotificationPermissionAction {
    SHOW_PROMPT,
    OPEN_SETTINGS,
    NONE,
}

object NotificationPermissionDecision {
    fun onHomeEntered(
        state: NotificationPermissionState,
        wasPromptAnswered: Boolean,
    ): NotificationPermissionAction =
        when {
            !state.requiresRuntimePermission || state.isGranted -> NotificationPermissionAction.NONE
            // 안내에 답한 적이 있어도 다시 띄우면 나중에를 누른 사용자도 앱을 열 때마다 안내를 보게 되므로, 자동 안내는 한 번만 적용
            wasPromptAnswered -> NotificationPermissionAction.NONE
            else -> NotificationPermissionAction.SHOW_PROMPT
        }

    fun onSettingsMenuClicked(
        state: NotificationPermissionState,
        wasPermissionRequested: Boolean,
    ): NotificationPermissionAction =
        when {
            // 권한 요청이 없는 기기나 이미 허용한 사용자는 알림을 끄거나 채널을 바꾸려는 것이므로, 앱 알림 설정으로 이동
            !state.requiresRuntimePermission || state.isGranted -> NotificationPermissionAction.OPEN_SETTINGS
            // rationale은 한 번도 묻지 않았을 때도 false이므로, 시스템 창을 띄운 적이 없으면 안내 후 요청
            state.shouldShowRationale || !wasPermissionRequested -> NotificationPermissionAction.SHOW_PROMPT
            // 다시 묻지 않음이나 두 번 거부 뒤에는 시스템 창이 뜨지 않으므로, 앱 알림 설정에서 켜도록 이동
            else -> NotificationPermissionAction.OPEN_SETTINGS
        }
}
