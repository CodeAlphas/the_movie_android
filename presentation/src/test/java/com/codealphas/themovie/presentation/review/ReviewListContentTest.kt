package com.codealphas.themovie.presentation.review

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.codealphas.themovie.core.android.theme.TheMovieTheme
import com.codealphas.themovie.domain.review.Review
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
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
}

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
