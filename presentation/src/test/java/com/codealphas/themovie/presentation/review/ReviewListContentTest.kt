package com.codealphas.themovie.presentation.review

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.codealphas.themovie.core.android.theme.TheMovieTheme
import com.codealphas.themovie.domain.review.Review
import com.codealphas.themovie.presentation.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
class ReviewListContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val originalTimeZone = TimeZone.getDefault()

    // 표시 문자열은 기기 시간대를 따라 달라지므로, 어느 기기와 CI에서도 같은 문자열을 기대하도록 시간대를 한국 시간으로 고정
    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun `감상문이 있으면 createdAt이 아니라 updatedAt을 연월일 시분 형식으로 보여야 한다`() {
        composeRule.setContent {
            TheMovieTheme {
                ReviewListContent(
                    state = ReviewListUiState(reviews = listOf(REVIEW), isSyncing = false),
                    onIntent = {},
                    onNavigateUp = {},
                    onNavigateToEdit = {},
                )
            }
        }

        composeRule.onNodeWithText("2026/10/02 12:00").assertIsDisplayed()
        composeRule.onNodeWithText("2026/09/30 08:05").assertDoesNotExist()
    }

    @Test
    fun `로그아웃 확인 창을 띄우면 서버에 반영하지 않은 감상문 변경 수가 담긴 안내 문구를 보여야 한다`() {
        setLogoutConfirmContent()

        composeRule.onNodeWithText(string(R.string.logout_pending_reviews_message, PENDING_COUNT)).assertIsDisplayed()
    }

    @Test
    fun `로그아웃 확인 창에서 로그아웃 버튼을 누르면 LogoutConfirmed를 보내야 한다`() {
        val intents = setLogoutConfirmContent()

        composeRule.onNodeWithText(string(R.string.common_logout)).performClick()

        assertEquals(listOf<ReviewListIntent>(ReviewListIntent.LogoutConfirmed), intents)
    }

    @Test
    fun `로그아웃 확인 창에서 취소 버튼을 누르면 LogoutDismissed를 보내야 한다`() {
        val intents = setLogoutConfirmContent()

        composeRule.onNodeWithText(string(R.string.logout_pending_reviews_cancel)).performClick()

        assertEquals(listOf<ReviewListIntent>(ReviewListIntent.LogoutDismissed), intents)
    }

    private fun setLogoutConfirmContent(): List<ReviewListIntent> {
        val intents = mutableListOf<ReviewListIntent>()
        composeRule.setContent {
            TheMovieTheme {
                ReviewListContent(
                    state =
                        ReviewListUiState(
                            reviews = emptyList(),
                            isSyncing = false,
                            logoutPendingReviewCount = PENDING_COUNT,
                        ),
                    onIntent = { intents += it },
                    onNavigateUp = {},
                    onNavigateToEdit = {},
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

// millis만 봐서는 기대 문자열과 맞는지 알 수 없으므로,
// createdAt은 2026-09-30 08:05, updatedAt은 2026-10-02 12:00(한국 시간)의 millis로 적용
private const val CREATED_AT = 1_790_723_100_000L
private const val UPDATED_AT = 1_790_910_000_000L

private val REVIEW =
    Review(
        title = "기생충",
        imageUrl = "",
        content = "잘 봤다",
        createdAt = CREATED_AT,
        updatedAt = UPDATED_AT,
        rating = 9.0,
        storageFileName = "",
        id = "-review",
    )
