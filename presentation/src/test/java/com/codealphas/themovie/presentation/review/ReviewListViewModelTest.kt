package com.codealphas.themovie.presentation.review

import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.auth.AuthRepository
import com.codealphas.themovie.domain.auth.AuthResult
import com.codealphas.themovie.domain.auth.LogoutUseCase
import com.codealphas.themovie.domain.result.Outcome
import com.codealphas.themovie.domain.review.Review
import com.codealphas.themovie.domain.review.ReviewDraft
import com.codealphas.themovie.domain.review.ReviewError
import com.codealphas.themovie.domain.review.ReviewRepository
import com.codealphas.themovie.domain.review.ReviewResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewListViewModelTest {
    @Test
    fun `Room 목록이 바뀌면 바뀐 목록을 상태에 담아야 한다`() {
        val repository = FakeReviewListRepository()
        runReviewListTest(repository) { viewModel ->
            assertNull(viewModel.state.value.reviews)

            runCurrent()
            assertEquals(emptyList<Review>(), viewModel.state.value.reviews)

            repository.reviews.value = listOf(REVIEW)
            runCurrent()
            assertEquals(listOf(REVIEW), viewModel.state.value.reviews)
        }
    }

    @Test
    fun `서버 동기화가 끝나기 전이면 Room 목록을 받기 전부터 동기화 중 상태여야 한다`() {
        val repository = FakeReviewListRepository(syncDone = CompletableDeferred())
        runReviewListTest(repository) { viewModel ->
            assertEquals(ReviewListUiState(reviews = null, isSyncing = true), viewModel.state.value)

            runCurrent()

            assertEquals(ReviewListUiState(reviews = emptyList(), isSyncing = true), viewModel.state.value)
        }
    }

    @Test
    fun `서버 동기화 중에 Room 목록이 바뀌면 바뀐 목록과 함께 동기화 중 상태를 유지해야 한다`() {
        val repository = FakeReviewListRepository(syncDone = CompletableDeferred())
        runReviewListTest(repository) { viewModel ->
            runCurrent()

            repository.reviews.value = listOf(REVIEW)
            runCurrent()

            assertEquals(ReviewListUiState(reviews = listOf(REVIEW), isSyncing = true), viewModel.state.value)
        }
    }

    @Test
    fun `서버 동기화에 성공하면 동기화 중 상태가 끝나야 한다`() {
        val syncDone = CompletableDeferred<Unit>()
        val repository = FakeReviewListRepository(syncResult = Outcome.Success(Unit), syncDone = syncDone)
        runReviewListTest(repository) { viewModel ->
            runCurrent()

            syncDone.complete(Unit)
            runCurrent()

            assertEquals(ReviewListUiState(reviews = emptyList(), isSyncing = false), viewModel.state.value)
        }
    }

    @Test
    fun `서버 동기화에 실패하면 동기화 중 상태가 끝나야 한다`() {
        val repository = FakeReviewListRepository(syncResult = Outcome.Failure(ReviewError.Unknown))
        runReviewListTest(repository) { viewModel ->
            runCurrent()

            assertEquals(ReviewListUiState(reviews = emptyList(), isSyncing = false), viewModel.state.value)
        }
    }

    @Test
    fun `Room 감상문이 없을 때 동기화에 실패하면 실패 안내를 한 번 보내야 한다`() {
        val repository = FakeReviewListRepository(syncResult = Outcome.Failure(ReviewError.Unknown))
        runReviewListTest(repository) { viewModel ->
            var failedCount = 0
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.effect.collect { effect -> if (effect == ReviewListEffect.ShowSyncFailed) failedCount++ }
            }

            runCurrent()

            assertEquals(1, repository.syncCalls)
            assertEquals(1, failedCount)
        }
    }

    @Test
    fun `Room 감상문이 있을 때 동기화에 실패하면 실패 안내를 보내지 않아야 한다`() {
        val repository = FakeReviewListRepository(syncResult = Outcome.Failure(ReviewError.Unknown))
        repository.reviews.value = listOf(REVIEW)
        runReviewListTest(repository) { viewModel ->
            val effects = collectEffects(viewModel)

            runCurrent()

            assertEquals(1, repository.syncCalls)
            assertEquals(ReviewListUiState(reviews = listOf(REVIEW), isSyncing = false), viewModel.state.value)
            assertEquals(emptyList<ReviewListEffect>(), effects)
        }
    }

    @Test
    fun `동기화에 성공하면 실패 안내를 보내지 않아야 한다`() {
        val repository = FakeReviewListRepository(syncResult = Outcome.Success(Unit))
        runReviewListTest(repository) { viewModel ->
            var failedCount = 0
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.effect.collect { effect -> if (effect == ReviewListEffect.ShowSyncFailed) failedCount++ }
            }

            runCurrent()

            assertEquals(1, repository.syncCalls)
            assertEquals(0, failedCount)
        }
    }

    @Test
    fun `감상문을 지우면 Repository에 그 감상문 삭제를 요청해야 한다`() {
        val repository = FakeReviewListRepository()
        runReviewListTest(repository) { viewModel ->
            viewModel.onIntent(ReviewListIntent.DeleteClicked(REVIEW))
            runCurrent()

            assertEquals(listOf(REVIEW), repository.deleted)
        }
    }

    @Test
    fun `감상문을 지우면 삭제가 끝난 뒤에 삭제했다는 안내를 보내야 한다`() {
        val deleteDone = CompletableDeferred<Unit>()
        val repository = FakeReviewListRepository(deleteDone = deleteDone)
        runReviewListTest(repository) { viewModel ->
            val effects = mutableListOf<ReviewListEffect>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.effect.collect { effects += it }
            }

            viewModel.onIntent(ReviewListIntent.DeleteClicked(REVIEW))
            runCurrent()
            assertEquals(emptyList<ReviewListEffect>(), effects)

            deleteDone.complete(Unit)
            runCurrent()
            assertEquals(listOf(ReviewListEffect.ShowDeleted), effects)
        }
    }

    @Test
    fun `감상문을 지우지 못하면 삭제했다는 안내 대신 삭제하지 못했다는 안내를 보내야 한다`() {
        val repository = FakeReviewListRepository(deleteResult = Outcome.Failure(ReviewError.Unknown))
        runReviewListTest(repository) { viewModel ->
            val effects = mutableListOf<ReviewListEffect>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.effect.collect { effects += it }
            }

            viewModel.onIntent(ReviewListIntent.DeleteClicked(REVIEW))
            runCurrent()

            assertEquals(listOf(ReviewListEffect.ShowDeleteFailed), effects)
        }
    }

    @Test
    fun `로그아웃하면 Room 삭제가 끝난 뒤에 로그아웃 완료를 보내야 한다`() {
        val deleteAllDone = CompletableDeferred<Unit>()
        val repository = FakeReviewListRepository(deleteAllDone = deleteAllDone)
        val authRepository = FakeReviewListAuthRepository()
        runReviewListTest(repository, authRepository) { viewModel ->
            var completedCount = 0
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.effect.collect { effect -> if (effect == ReviewListEffect.NavigateToLogin) completedCount++ }
            }

            viewModel.onIntent(ReviewListIntent.LogoutClicked)
            runCurrent()
            assertEquals(0, authRepository.signOutCalls)
            assertEquals(0, completedCount)

            deleteAllDone.complete(Unit)
            runCurrent()
            assertEquals(1, repository.deleteAllCalls)
            assertEquals(1, authRepository.signOutCalls)
            assertEquals(1, completedCount)
        }
    }

    @Test
    fun `서버 동기화 중에 로그아웃하면 동기화를 멈춘 뒤 감상문을 지워야 한다`() {
        val repository = FakeReviewListRepository(syncDone = CompletableDeferred())
        runReviewListTest(repository) { viewModel ->
            runCurrent()

            viewModel.onIntent(ReviewListIntent.LogoutClicked)
            runCurrent()

            assertEquals(listOf("syncCancelled", "deleteAll"), repository.calls)
        }
    }

    @Test
    fun `서버에 반영하지 않은 감상문 변경이 있을 때 로그아웃 버튼을 누르면 로그아웃하지 않고 변경 수와 함께 확인 창을 띄워야 한다`() {
        val repository = FakeReviewListRepository(pendingCount = 2)
        val authRepository = FakeReviewListAuthRepository()
        runReviewListTest(repository, authRepository) { viewModel ->
            val effects = collectEffects(viewModel)

            viewModel.onIntent(ReviewListIntent.LogoutClicked)
            runCurrent()

            assertEquals(2, viewModel.state.value.logoutPendingReviewCount)
            assertEquals(0, repository.deleteAllCalls)
            assertEquals(0, authRepository.signOutCalls)
            assertEquals(emptyList<ReviewListEffect>(), effects)
        }
    }

    @Test
    fun `서버에 반영하지 않은 감상문 변경이 없을 때 로그아웃 버튼을 누르면 확인 창을 띄우지 않고 로그아웃해야 한다`() {
        val repository = FakeReviewListRepository()
        val authRepository = FakeReviewListAuthRepository()
        runReviewListTest(repository, authRepository) { viewModel ->
            val effects = collectEffects(viewModel)

            viewModel.onIntent(ReviewListIntent.LogoutClicked)
            runCurrent()

            assertNull(viewModel.state.value.logoutPendingReviewCount)
            assertEquals(1, authRepository.signOutCalls)
            assertEquals(listOf<ReviewListEffect>(ReviewListEffect.NavigateToLogin), effects)
        }
    }

    @Test
    fun `로그아웃 확인 창에서 로그아웃 버튼을 누르면 확인 창을 닫고 감상문을 지운 뒤 로그아웃해야 한다`() {
        val repository = FakeReviewListRepository(pendingCount = 2)
        val authRepository = FakeReviewListAuthRepository()
        runReviewListTest(repository, authRepository) { viewModel ->
            val effects = collectEffects(viewModel)
            viewModel.onIntent(ReviewListIntent.LogoutClicked)
            runCurrent()

            viewModel.onIntent(ReviewListIntent.LogoutConfirmed)
            runCurrent()

            assertNull(viewModel.state.value.logoutPendingReviewCount)
            assertEquals(1, repository.deleteAllCalls)
            assertEquals(1, authRepository.signOutCalls)
            assertEquals(listOf<ReviewListEffect>(ReviewListEffect.NavigateToLogin), effects)
        }
    }

    @Test
    fun `로그아웃 확인 창에서 로그아웃 버튼을 두 번 누르면 한 번만 로그아웃해야 한다`() {
        val deleteAllDone = CompletableDeferred<Unit>()
        val repository = FakeReviewListRepository(deleteAllDone = deleteAllDone, pendingCount = 2)
        val authRepository = FakeReviewListAuthRepository()
        runReviewListTest(repository, authRepository) { viewModel ->
            val effects = collectEffects(viewModel)
            viewModel.onIntent(ReviewListIntent.LogoutClicked)
            runCurrent()

            viewModel.onIntent(ReviewListIntent.LogoutConfirmed)
            viewModel.onIntent(ReviewListIntent.LogoutConfirmed)
            deleteAllDone.complete(Unit)
            runCurrent()

            assertEquals(1, repository.deleteAllCalls)
            assertEquals(1, authRepository.signOutCalls)
            assertEquals(listOf<ReviewListEffect>(ReviewListEffect.NavigateToLogin), effects)
        }
    }

    @Test
    fun `로그아웃 확인 창에서 취소하면 확인 창을 닫고 로그아웃하지 않아야 한다`() {
        val repository = FakeReviewListRepository(pendingCount = 2)
        val authRepository = FakeReviewListAuthRepository()
        runReviewListTest(repository, authRepository) { viewModel ->
            val effects = collectEffects(viewModel)
            viewModel.onIntent(ReviewListIntent.LogoutClicked)
            runCurrent()

            viewModel.onIntent(ReviewListIntent.LogoutDismissed)
            runCurrent()

            assertNull(viewModel.state.value.logoutPendingReviewCount)
            assertEquals(0, repository.deleteAllCalls)
            assertEquals(0, authRepository.signOutCalls)
            assertEquals(emptyList<ReviewListEffect>(), effects)
        }
    }

    @Test
    fun `서버 동기화 중에 로그아웃 확인 창에서 취소하면 동기화를 멈추지 않고 동기화 중 상태를 끝내야 한다`() {
        val syncDone = CompletableDeferred<Unit>()
        val repository = FakeReviewListRepository(syncDone = syncDone, pendingCount = 2)
        runReviewListTest(repository) { viewModel ->
            runCurrent()
            viewModel.onIntent(ReviewListIntent.LogoutClicked)
            runCurrent()

            viewModel.onIntent(ReviewListIntent.LogoutDismissed)
            syncDone.complete(Unit)
            runCurrent()

            assertEquals(emptyList<String>(), repository.calls)
            assertFalse(viewModel.state.value.isSyncing)
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun TestScope.collectEffects(viewModel: ReviewListViewModel): List<ReviewListEffect> {
    val effects = mutableListOf<ReviewListEffect>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        viewModel.effect.collect { effects += it }
    }
    return effects
}

private val REVIEW =
    Review(
        title = "기생충",
        imageUrl = "",
        content = "잘 봤다",
        createdAt = 1_704_132_240_000L,
        updatedAt = 1_704_132_240_000L,
        rating = 9.0,
        storageFileName = "",
        id = "-review",
    )

private fun runReviewListTest(
    repository: FakeReviewListRepository,
    authRepository: FakeReviewListAuthRepository = FakeReviewListAuthRepository(),
    body: suspend TestScope.(ReviewListViewModel) -> Unit,
) = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    val viewModel = ReviewListViewModel(repository, LogoutUseCase(authRepository, repository))
    try {
        body(viewModel)
    } finally {
        viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
    }
}

private class FakeReviewListRepository(
    private val syncResult: ReviewResult = Outcome.Success(Unit),
    private val deleteAllDone: CompletableDeferred<Unit> = CompletableDeferred(Unit),
    private val syncDone: CompletableDeferred<Unit> = CompletableDeferred(Unit),
    private val deleteDone: CompletableDeferred<Unit> = CompletableDeferred(Unit),
    private val deleteResult: ReviewResult = Outcome.Success(Unit),
    private val pendingCount: Int = 0,
) : ReviewRepository {
    val reviews = MutableStateFlow(emptyList<Review>())
    val deleted = mutableListOf<Review>()
    val calls = mutableListOf<String>()
    var syncCalls: Int = 0
    var deleteAllCalls: Int = 0

    override fun observeAll(): Flow<List<Review>> = reviews

    override suspend fun syncFromRemote(): ReviewResult {
        syncCalls += 1
        try {
            syncDone.await()
        } catch (e: CancellationException) {
            calls += "syncCancelled"
            throw e
        }
        return syncResult
    }

    override suspend fun getById(id: String): Review? = error("사용하지 않음")

    override suspend fun save(draft: ReviewDraft): ReviewResult = error("사용하지 않음")

    override suspend fun delete(review: Review): ReviewResult {
        deleteDone.await()
        deleted += review
        return deleteResult
    }

    override suspend fun deleteAll() {
        deleteAllDone.await()
        deleteAllCalls += 1
        calls += "deleteAll"
    }

    override suspend fun countPendingChanges(): Int = pendingCount

    override fun cancelPendingSync() = Unit
}

private class FakeReviewListAuthRepository : AuthRepository {
    var signOutCalls: Int = 0

    override suspend fun signIn(
        email: String,
        password: String,
    ): AuthResult = error("사용하지 않음")

    override suspend fun signUp(
        email: String,
        password: String,
    ): AuthResult = error("사용하지 않음")

    override fun currentUserId(): String? = error("사용하지 않음")

    override fun signOut() {
        signOutCalls += 1
    }
}
