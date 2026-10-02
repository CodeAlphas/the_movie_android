package com.codealphas.themovie.presentation.home

import android.os.Build
import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.auth.AuthRepository
import com.codealphas.themovie.domain.auth.AuthResult
import com.codealphas.themovie.domain.auth.LogoutUseCase
import com.codealphas.themovie.domain.notification.NotificationPromptRepository
import com.codealphas.themovie.domain.review.Review
import com.codealphas.themovie.domain.review.ReviewDraft
import com.codealphas.themovie.domain.review.ReviewRepository
import com.codealphas.themovie.domain.review.ReviewResult
import com.codealphas.themovie.presentation.notification.NotificationPermissionState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @Test
    fun `로그아웃하면 Room 삭제가 끝난 뒤에 로그아웃 완료를 보내야 한다`() {
        val deleteAllDone = CompletableDeferred<Unit>()
        val authRepository = FakeHomeAuthRepository(userId = "uid")
        val reviewRepository = FakeHomeReviewRepository(deleteAllDone)
        runHomeTest(authRepository = authRepository, reviewRepository = reviewRepository) { viewModel ->
            var completedCount = 0
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.effect.collect { if (it == HomeEffect.NavigateToLogin) completedCount++ }
            }

            viewModel.onIntent(HomeIntent.LogoutClicked)
            runCurrent()
            assertEquals(0, authRepository.signOutCalls)
            assertEquals(0, completedCount)

            deleteAllDone.complete(Unit)
            runCurrent()
            assertEquals(1, reviewRepository.deleteAllCalls)
            assertEquals(1, authRepository.signOutCalls)
            assertEquals(1, completedCount)
        }
    }

    @Test
    fun `알림 권한 안내에 답한 적이 없는 상태로 메인에 들어오면 알림 권한 안내 창을 띄워야 한다`() {
        val promptRepository = FakeNotificationPromptRepository(promptShown = false)
        runHomeTest(promptRepository = promptRepository) { viewModel ->
            viewModel.onIntent(HomeIntent.Entered(permissionState()))
            runCurrent()

            assertTrue(viewModel.state.value.showNotificationPrompt)
        }
    }

    @Test
    fun `알림 권한 안내에 답한 적이 있는 상태로 메인에 들어오면 알림 권한 안내 창을 띄우지 않아야 한다`() {
        val promptRepository = FakeNotificationPromptRepository(promptShown = true)
        runHomeTest(promptRepository = promptRepository) { viewModel ->
            val effects = collectEffects(viewModel)

            viewModel.onIntent(HomeIntent.Entered(permissionState()))
            runCurrent()

            assertFalse(viewModel.state.value.showNotificationPrompt)
            assertEquals(emptyList<HomeEffect>(), effects)
        }
    }

    @Test
    fun `알림 권한을 두 번 거부한 상태로 알림 설정 메뉴를 누르면 앱 알림 설정 이벤트를 보내야 한다`() {
        // 두 번 거부하면 rationale이 한 번도 묻지 않았을 때처럼 false로 돌아가므로,
        // 시스템 창을 띄운 적이 있다는 저장 값과 rationale false 조합으로 두 번 거부한 상태 적용
        val promptRepository = FakeNotificationPromptRepository(permissionRequested = true)
        runHomeTest(promptRepository = promptRepository) { viewModel ->
            val effects = collectEffects(viewModel)

            viewModel.onIntent(HomeIntent.NotificationSettingsClicked(permissionState()))
            runCurrent()

            assertEquals(listOf<HomeEffect>(HomeEffect.OpenNotificationSettings), effects)
        }
    }

    @Test
    fun `알림 권한 안내를 수락하면 안내 표시와 시스템 창 표시를 모두 저장해야 한다`() {
        val promptRepository = FakeNotificationPromptRepository()
        runHomeTest(promptRepository = promptRepository) { viewModel ->
            viewModel.onIntent(HomeIntent.NotificationPromptAccepted)
            runCurrent()

            assertEquals(listOf("markPromptShown", "markPermissionRequested"), promptRepository.marks)
        }
    }

    @Test
    fun `알림 권한 안내를 수락하면 안내 창을 닫고 알림 권한을 요청해야 한다`() {
        val promptRepository = FakeNotificationPromptRepository(promptShown = false)
        runHomeTest(promptRepository = promptRepository) { viewModel ->
            val effects = collectEffects(viewModel)
            viewModel.onIntent(HomeIntent.Entered(permissionState()))
            runCurrent()

            viewModel.onIntent(HomeIntent.NotificationPromptAccepted)
            runCurrent()

            assertFalse(viewModel.state.value.showNotificationPrompt)
            assertEquals(listOf<HomeEffect>(HomeEffect.RequestNotificationPermission), effects)
        }
    }

    @Test
    fun `알림 권한 안내를 거절하면 안내 표시만 저장해야 한다`() {
        val promptRepository = FakeNotificationPromptRepository()
        runHomeTest(promptRepository = promptRepository) { viewModel ->
            viewModel.onIntent(HomeIntent.NotificationPromptDeclined)
            runCurrent()

            assertEquals(listOf("markPromptShown"), promptRepository.marks)
        }
    }

    @Test
    fun `알림 권한 안내를 거절하면 안내 창을 닫고 알림 권한을 요청하지 않아야 한다`() {
        val promptRepository = FakeNotificationPromptRepository(promptShown = false)
        runHomeTest(promptRepository = promptRepository) { viewModel ->
            val effects = collectEffects(viewModel)
            viewModel.onIntent(HomeIntent.Entered(permissionState()))
            runCurrent()

            viewModel.onIntent(HomeIntent.NotificationPromptDeclined)
            runCurrent()

            assertFalse(viewModel.state.value.showNotificationPrompt)
            assertEquals(emptyList<HomeEffect>(), effects)
        }
    }
}

// 권한이 없고 rationale도 false인 Android 13 상태는 저장된 표시 여부에 따라 판단 결과가 갈리므로,
// 저장소 값만 바꿔 분기를 확인하도록 이 상태를 기본값으로 적용
private fun permissionState(): NotificationPermissionState =
    NotificationPermissionState(
        sdkInt = Build.VERSION_CODES.TIRAMISU,
        isGranted = false,
        shouldShowRationale = false,
    )

private fun runHomeTest(
    authRepository: FakeHomeAuthRepository = FakeHomeAuthRepository(userId = "uid"),
    reviewRepository: FakeHomeReviewRepository = FakeHomeReviewRepository(),
    promptRepository: FakeNotificationPromptRepository = FakeNotificationPromptRepository(),
    body: suspend TestScope.(HomeViewModel) -> Unit,
) = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    val viewModel =
        HomeViewModel(
            logoutUseCase = LogoutUseCase(authRepository, reviewRepository),
            notificationPromptRepository = promptRepository,
        )
    try {
        body(viewModel)
    } finally {
        viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun TestScope.collectEffects(viewModel: HomeViewModel): List<HomeEffect> {
    val effects = mutableListOf<HomeEffect>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        viewModel.effect.collect { effects += it }
    }
    return effects
}

private class FakeHomeAuthRepository(
    private val userId: String?,
) : AuthRepository {
    var signOutCalls: Int = 0

    override suspend fun signIn(
        email: String,
        password: String,
    ): AuthResult = error("사용하지 않음")

    override suspend fun signUp(
        email: String,
        password: String,
    ): AuthResult = error("사용하지 않음")

    override fun currentUserId(): String? = userId

    override fun signOut() {
        signOutCalls += 1
    }
}

private class FakeHomeReviewRepository(
    private val deleteAllDone: CompletableDeferred<Unit> = CompletableDeferred(Unit),
) : ReviewRepository {
    var deleteAllCalls: Int = 0

    override fun observeAll(): Flow<List<Review>> = error("사용하지 않음")

    override suspend fun syncFromRemote(): ReviewResult = error("사용하지 않음")

    override suspend fun getById(id: Int): Review? = error("사용하지 않음")

    override suspend fun save(draft: ReviewDraft): ReviewResult = error("사용하지 않음")

    override suspend fun delete(review: Review) = error("사용하지 않음")

    override suspend fun deleteAll() {
        deleteAllDone.await()
        deleteAllCalls += 1
    }
}

private class FakeNotificationPromptRepository(
    private val promptShown: Boolean = false,
    private val permissionRequested: Boolean = false,
) : NotificationPromptRepository {
    val marks = mutableListOf<String>()

    override suspend fun wasPromptShown(): Boolean = promptShown

    override suspend fun markPromptShown() {
        marks += "markPromptShown"
    }

    override suspend fun wasPermissionRequested(): Boolean = permissionRequested

    override suspend fun markPermissionRequested() {
        marks += "markPermissionRequested"
    }
}
