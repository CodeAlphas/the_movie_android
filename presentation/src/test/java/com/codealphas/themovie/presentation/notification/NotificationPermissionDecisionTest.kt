package com.codealphas.themovie.presentation.notification

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationPermissionDecisionTest {
    @Test
    fun `Android 12 이하에서 홈에 들어오면 안내하지 않아야 한다`() {
        val action = NotificationPermissionDecision.onHomeEntered(state(sdkInt = Build.VERSION_CODES.S_V2), false)

        assertEquals(NotificationPermissionAction.NONE, action)
    }

    @Test
    fun `권한이 이미 허용된 상태로 홈에 들어오면 안내하지 않아야 한다`() {
        val action = NotificationPermissionDecision.onHomeEntered(state(isGranted = true), false)

        assertEquals(NotificationPermissionAction.NONE, action)
    }

    @Test
    fun `안내에 답한 적이 없는 상태로 홈에 들어오면 안내를 띄워야 한다`() {
        val action = NotificationPermissionDecision.onHomeEntered(state(), wasPromptAnswered = false)

        assertEquals(NotificationPermissionAction.SHOW_PROMPT, action)
    }

    @Test
    fun `안내에 답한 적이 있는 상태로 홈에 들어오면 다시 안내하지 않아야 한다`() {
        val action =
            NotificationPermissionDecision.onHomeEntered(
                state(shouldShowRationale = true),
                wasPromptAnswered = true,
            )

        assertEquals(NotificationPermissionAction.NONE, action)
    }

    @Test
    fun `Android 12 이하에서 알림 설정 메뉴를 누르면 앱 알림 설정으로 보내야 한다`() {
        val action =
            NotificationPermissionDecision.onSettingsMenuClicked(
                state(sdkInt = Build.VERSION_CODES.S_V2),
                false,
            )

        assertEquals(NotificationPermissionAction.OPEN_SETTINGS, action)
    }

    @Test
    fun `권한이 허용된 상태에서 알림 설정 메뉴를 누르면 앱 알림 설정으로 보내야 한다`() {
        val action = NotificationPermissionDecision.onSettingsMenuClicked(state(isGranted = true), true)

        assertEquals(NotificationPermissionAction.OPEN_SETTINGS, action)
    }

    @Test
    fun `시스템 창을 띄운 적이 없으면 알림 설정 메뉴에서 안내를 띄워야 한다`() {
        val action = NotificationPermissionDecision.onSettingsMenuClicked(state(), wasPermissionRequested = false)

        assertEquals(NotificationPermissionAction.SHOW_PROMPT, action)
    }

    @Test
    fun `한 번 거부해 rationale이 true면 알림 설정 메뉴에서 안내를 띄워야 한다`() {
        val action =
            NotificationPermissionDecision.onSettingsMenuClicked(
                state(shouldShowRationale = true),
                wasPermissionRequested = true,
            )

        assertEquals(NotificationPermissionAction.SHOW_PROMPT, action)
    }

    @Test
    fun `물은 적이 있고 rationale이 false면 알림 설정 메뉴에서 앱 알림 설정으로 보내야 한다`() {
        val action = NotificationPermissionDecision.onSettingsMenuClicked(state(), wasPermissionRequested = true)

        assertEquals(NotificationPermissionAction.OPEN_SETTINGS, action)
    }

    private fun state(
        sdkInt: Int = Build.VERSION_CODES.TIRAMISU,
        isGranted: Boolean = false,
        shouldShowRationale: Boolean = false,
    ) = NotificationPermissionState(sdkInt, isGranted, shouldShowRationale)
}
