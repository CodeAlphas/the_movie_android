package com.codealphas.themovie.auth

import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.auth.AuthError
import com.codealphas.themovie.domain.auth.AuthRepository
import com.codealphas.themovie.domain.auth.AuthResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
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
class LoginViewModelTest {
    @Test
    fun `이메일이나 비밀번호가 비어 있거나 공백뿐이면 입력 안내를 보내야 한다`() {
        val repository = FakeLoginAuthRepository()
        runLoginTest(repository) { viewModel ->
            val events = collectLoginEvents(viewModel)

            viewModel.signIn(" ", "password")
            viewModel.signIn("user@example.com", "")
            runCurrent()

            assertEquals(0, repository.signInCalls)
            assertEquals(listOf(LoginEvent.ShowInvalidInput, LoginEvent.ShowInvalidInput), events)
            assertFalse(viewModel.uiState.value.isLoading)
        }
    }

    @Test
    fun `로그인 중에 로그인 버튼을 다시 누르면 로그인을 한 번만 요청해야 한다`() {
        val result = CompletableDeferred<AuthResult>()
        val repository = FakeLoginAuthRepository(signInResult = result)
        runLoginTest(repository) { viewModel ->
            viewModel.signIn("user@example.com", "password")
            runCurrent()
            viewModel.signIn("user@example.com", "password")
            runCurrent()

            assertEquals(1, repository.signInCalls)
            assertTrue(viewModel.uiState.value.isLoading)
            result.complete(AuthResult.Success)
        }
    }

    @Test
    fun `로그인에 성공하면 로딩을 끝내고 메인 이동 이벤트를 보내야 한다`() {
        val repository = FakeLoginAuthRepository(signInResult = CompletableDeferred(AuthResult.Success))
        runLoginTest(repository) { viewModel ->
            val events = collectLoginEvents(viewModel)

            viewModel.signIn("user@example.com", "password")
            runCurrent()

            assertEquals(listOf<LoginEvent>(LoginEvent.NavigateToMain), events)
            assertFalse(viewModel.uiState.value.isLoading)
        }
    }

    @Test
    fun `로그인에 실패하면 로딩을 끝내고 실패 이유를 보내야 한다`() {
        val failure = AuthResult.Failure(AuthError.InvalidCredentials)
        val repository = FakeLoginAuthRepository(signInResult = CompletableDeferred(failure))
        runLoginTest(repository) { viewModel ->
            val events = collectLoginEvents(viewModel)

            viewModel.signIn("user@example.com", "password")
            runCurrent()

            assertEquals(listOf<LoginEvent>(LoginEvent.ShowError(AuthError.InvalidCredentials)), events)
            assertFalse(viewModel.uiState.value.isLoading)
        }
    }
}

private fun runLoginTest(
    repository: FakeLoginAuthRepository,
    body: suspend TestScope.(LoginViewModel) -> Unit,
) = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    val viewModel = LoginViewModel(repository)
    try {
        body(viewModel)
    } finally {
        viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun TestScope.collectLoginEvents(viewModel: LoginViewModel): List<LoginEvent> {
    val events = mutableListOf<LoginEvent>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        viewModel.events.collect { events += it }
    }
    return events
}

private class FakeLoginAuthRepository(
    private val signInResult: CompletableDeferred<AuthResult> = CompletableDeferred(),
) : AuthRepository {
    var signInCalls: Int = 0

    override suspend fun signIn(
        email: String,
        password: String,
    ): AuthResult {
        signInCalls += 1
        return signInResult.await()
    }

    override suspend fun signUp(
        email: String,
        password: String,
    ): AuthResult = error("사용하지 않음")

    override fun currentUserId(): String? = error("사용하지 않음")

    override fun signOut() = error("사용하지 않음")
}
