package com.codealphas.themovie.presentation.notification

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationPromptDecisionTest {
    @Test
    fun `Android 12 이하에서 메인에 들어오면 안내하지 않아야 한다`() {
        val action = NotificationPromptDecision.onMainEntered(state(sdkInt = Build.VERSION_CODES.S_V2), false)

        assertEquals(NotificationPromptAction.NONE, action)
    }

    @Test
    fun `권한이 이미 허용된 상태로 메인에 들어오면 안내하지 않아야 한다`() {
        val action = NotificationPromptDecision.onMainEntered(state(isGranted = true), false)

        assertEquals(NotificationPromptAction.NONE, action)
    }

    @Test
    fun `안내에 답한 적이 없는 상태로 메인에 들어오면 안내를 띄워야 한다`() {
        val action = NotificationPromptDecision.onMainEntered(state(), wasPromptShown = false)

        assertEquals(NotificationPromptAction.SHOW_PROMPT, action)
    }

    @Test
    fun `안내에 답한 적이 있는 상태로 메인에 들어오면 다시 안내하지 않아야 한다`() {
        val action = NotificationPromptDecision.onMainEntered(state(shouldShowRationale = true), wasPromptShown = true)

        assertEquals(NotificationPromptAction.NONE, action)
    }

    @Test
    fun `Android 12 이하에서 알림 설정 메뉴를 누르면 앱 알림 설정으로 보내야 한다`() {
        val action = NotificationPromptDecision.onSettingsMenuClicked(state(sdkInt = Build.VERSION_CODES.S_V2), false)

        assertEquals(NotificationPromptAction.OPEN_SETTINGS, action)
    }

    @Test
    fun `권한이 허용된 상태에서 알림 설정 메뉴를 누르면 앱 알림 설정으로 보내야 한다`() {
        val action = NotificationPromptDecision.onSettingsMenuClicked(state(isGranted = true), true)

        assertEquals(NotificationPromptAction.OPEN_SETTINGS, action)
    }

    @Test
    fun `시스템 창을 띄운 적이 없으면 알림 설정 메뉴에서 안내를 띄워야 한다`() {
        val action = NotificationPromptDecision.onSettingsMenuClicked(state(), wasPermissionRequested = false)

        assertEquals(NotificationPromptAction.SHOW_PROMPT, action)
    }

    @Test
    fun `한 번 거부해 rationale이 true면 알림 설정 메뉴에서 안내를 띄워야 한다`() {
        val action =
            NotificationPromptDecision.onSettingsMenuClicked(
                state(shouldShowRationale = true),
                wasPermissionRequested = true,
            )

        assertEquals(NotificationPromptAction.SHOW_PROMPT, action)
    }

    @Test
    fun `물은 적이 있고 rationale이 false면 알림 설정 메뉴에서 앱 알림 설정으로 보내야 한다`() {
        val action = NotificationPromptDecision.onSettingsMenuClicked(state(), wasPermissionRequested = true)

        assertEquals(NotificationPromptAction.OPEN_SETTINGS, action)
    }

    private fun state(
        sdkInt: Int = Build.VERSION_CODES.TIRAMISU,
        isGranted: Boolean = false,
        shouldShowRationale: Boolean = false,
    ) = NotificationPermissionState(sdkInt, isGranted, shouldShowRationale)
}
