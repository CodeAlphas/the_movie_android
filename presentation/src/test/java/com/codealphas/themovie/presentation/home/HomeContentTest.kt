package com.codealphas.themovie.presentation.home

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.codealphas.themovie.core.android.theme.TheMovieTheme
import com.codealphas.themovie.presentation.R
import com.codealphas.themovie.presentation.home.navigation.HomeTab
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class HomeContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `로그아웃 확인 창을 띄우면 서버에 반영하지 않은 감상문 변경 수가 담긴 안내 문구를 보여야 한다`() {
        setLogoutConfirmContent()

        composeRule.onNodeWithText(string(R.string.logout_pending_reviews_message, PENDING_COUNT)).assertIsDisplayed()
    }

    @Test
    fun `로그아웃 확인 창에서 로그아웃 버튼을 누르면 LogoutConfirmed를 보내야 한다`() {
        val intents = setLogoutConfirmContent()

        composeRule.onNodeWithText(string(R.string.common_logout)).performClick()

        assertEquals(listOf<HomeIntent>(HomeIntent.LogoutConfirmed), intents)
    }

    @Test
    fun `로그아웃 확인 창에서 취소 버튼을 누르면 LogoutDismissed를 보내야 한다`() {
        val intents = setLogoutConfirmContent()

        composeRule.onNodeWithText(string(R.string.logout_pending_reviews_cancel)).performClick()

        assertEquals(listOf<HomeIntent>(HomeIntent.LogoutDismissed), intents)
    }

    private fun setLogoutConfirmContent(): List<HomeIntent> {
        val intents = mutableListOf<HomeIntent>()
        composeRule.setContent {
            TheMovieTheme {
                HomeContent(
                    state = HomeUiState(logoutPendingReviewCount = PENDING_COUNT),
                    selectedTab = HomeTab.POPULAR,
                    onTabSelected = {},
                    snackbarHostState = remember { SnackbarHostState() },
                    onIntent = { intents += it },
                    onNotificationSettingsClick = {},
                    content = {},
                )
            }
        }
        return intents
    }
}

private const val PENDING_COUNT = 3

private fun string(
    id: Int,
    vararg args: Any,
): String = RuntimeEnvironment.getApplication().getString(id, *args)
