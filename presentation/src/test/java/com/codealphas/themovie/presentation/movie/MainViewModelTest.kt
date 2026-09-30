package com.codealphas.themovie.presentation.movie

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
import com.codealphas.themovie.presentation.notification.NotificationPromptAction
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
class MainViewModelTest {
    @Test
    fun `로그인한 상태면 isSignedIn이 true여야 한다`() {
        runMainTest(authRepository = FakeMainAuthRepository(userId = "uid")) { viewModel ->
            assertTrue(viewModel.isSignedIn())
        }
    }

    @Test
    fun `로그인하지 않은 상태면 isSignedIn이 false여야 한다`() {
        runMainTest(authRepository = FakeMainAuthRepository(userId = null)) { viewModel ->
            assertFalse(viewModel.isSignedIn())
        }
    }

    @Test
    fun `로그아웃하면 Room 삭제가 끝난 뒤에 로그아웃 완료를 보내야 한다`() {
        val deleteAllDone = CompletableDeferred<Unit>()
        val authRepository = FakeMainAuthRepository(userId = "uid")
        val reviewRepository = FakeMainReviewRepository(deleteAllDone)
        runMainTest(authRepository = authRepository, reviewRepository = reviewRepository) { viewModel ->
            var completedCount = 0
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.logoutCompleted.collect { completedCount++ }
            }

            viewModel.logout()
            runCurrent()
            assertEquals(1, authRepository.signOutCalls)
            assertEquals(0, completedCount)

            deleteAllDone.complete(Unit)
            runCurrent()
            assertEquals(1, reviewRepository.deleteAllCalls)
            assertEquals(1, completedCount)
        }
    }

    @Test
    fun `알림 권한 안내에 답한 적이 없는 상태로 메인에 들어오면 알림 권한 안내 이벤트를 보내야 한다`() {
        val promptRepository = FakeNotificationPromptRepository(promptShown = false)
        runMainTest(promptRepository = promptRepository) { viewModel ->
            val actions = collectPromptActions(viewModel)

            viewModel.onMainEntered(permissionState())
            runCurrent()

            assertEquals(listOf(NotificationPromptAction.SHOW_RATIONALE), actions)
        }
    }

    @Test
    fun `알림 권한 안내에 답한 적이 있는 상태로 메인에 들어오면 이벤트를 보내지 않아야 한다`() {
        val promptRepository = FakeNotificationPromptRepository(promptShown = true)
        runMainTest(promptRepository = promptRepository) { viewModel ->
            val actions = collectPromptActions(viewModel)

            viewModel.onMainEntered(permissionState())
            runCurrent()

            assertEquals(emptyList<NotificationPromptAction>(), actions)
        }
    }

    @Test
    fun `알림 권한을 두 번 거부한 상태로 알림 설정 메뉴를 누르면 앱 알림 설정 이벤트를 보내야 한다`() {
        // 두 번 거부하면 rationale이 한 번도 묻지 않았을 때처럼 false로 돌아가므로,
        // 시스템 창을 띄운 적이 있다는 저장 값과 rationale false 조합으로 두 번 거부한 상태 적용
        val promptRepository = FakeNotificationPromptRepository(permissionRequested = true)
        runMainTest(promptRepository = promptRepository) { viewModel ->
            val actions = collectPromptActions(viewModel)

            viewModel.onNotificationSettingsClicked(permissionState())
            runCurrent()

            assertEquals(listOf(NotificationPromptAction.OPEN_SETTINGS), actions)
        }
    }

    @Test
    fun `알림 권한 안내를 수락하면 안내 표시와 시스템 창 표시를 모두 저장해야 한다`() {
        val promptRepository = FakeNotificationPromptRepository()
        runMainTest(promptRepository = promptRepository) { viewModel ->
            viewModel.onNotificationPromptAccepted()
            runCurrent()

            assertEquals(listOf("markPromptShown", "markPermissionRequested"), promptRepository.marks)
        }
    }

    @Test
    fun `알림 권한 안내를 거절하면 안내 표시만 저장해야 한다`() {
        val promptRepository = FakeNotificationPromptRepository()
        runMainTest(promptRepository = promptRepository) { viewModel ->
            viewModel.onNotificationPromptDeclined()
            runCurrent()

            assertEquals(listOf("markPromptShown"), promptRepository.marks)
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

private fun runMainTest(
    authRepository: FakeMainAuthRepository = FakeMainAuthRepository(userId = "uid"),
    reviewRepository: FakeMainReviewRepository = FakeMainReviewRepository(),
    promptRepository: FakeNotificationPromptRepository = FakeNotificationPromptRepository(),
    body: suspend TestScope.(MainViewModel) -> Unit,
) = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    val viewModel =
        MainViewModel(
            authRepository = authRepository,
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
private fun TestScope.collectPromptActions(viewModel: MainViewModel): List<NotificationPromptAction> {
    val actions = mutableListOf<NotificationPromptAction>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        viewModel.notificationPrompt.collect { actions += it }
    }
    return actions
}

private class FakeMainAuthRepository(
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

private class FakeMainReviewRepository(
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
