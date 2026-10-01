package com.codealphas.themovie.presentation.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.auth.AuthError
import com.codealphas.themovie.domain.auth.AuthRepository
import com.codealphas.themovie.domain.auth.AuthResult
import com.codealphas.themovie.domain.result.Outcome
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
            val effects = collectLoginEffects(viewModel)

            viewModel.signIn(" ", "password")
            viewModel.signIn("user@example.com", "")
            runCurrent()

            assertEquals(0, repository.signInCalls)
            assertEquals(listOf(LoginEffect.ShowInvalidInput, LoginEffect.ShowInvalidInput), effects)
            assertFalse(viewModel.state.value.isLoading)
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
            assertTrue(viewModel.state.value.isLoading)
            result.complete(Outcome.Success(Unit))
        }
    }

    @Test
    fun `로그인에 성공하면 로딩을 끝내고 홈 이동 이벤트를 보내야 한다`() {
        val repository = FakeLoginAuthRepository(signInResult = CompletableDeferred(Outcome.Success(Unit)))
        runLoginTest(repository) { viewModel ->
            val effects = collectLoginEffects(viewModel)

            viewModel.signIn("user@example.com", "password")
            runCurrent()

            assertEquals(listOf<LoginEffect>(LoginEffect.NavigateToHome), effects)
            assertFalse(viewModel.state.value.isLoading)
        }
    }

    @Test
    fun `로그인에 실패하면 로딩을 끝내고 실패 이유를 보내야 한다`() {
        val failure = Outcome.Failure(AuthError.InvalidCredentials)
        val repository = FakeLoginAuthRepository(signInResult = CompletableDeferred(failure))
        runLoginTest(repository) { viewModel ->
            val effects = collectLoginEffects(viewModel)

            viewModel.signIn("user@example.com", "password")
            runCurrent()

            assertEquals(listOf<LoginEffect>(LoginEffect.ShowError(AuthError.InvalidCredentials)), effects)
            assertFalse(viewModel.state.value.isLoading)
        }
    }

    @Test
    fun `가입하기를 누르면 가입 화면 이동 이벤트를 보내야 한다`() {
        val repository = FakeLoginAuthRepository()
        runLoginTest(repository) { viewModel ->
            val effects = collectLoginEffects(viewModel)

            viewModel.onIntent(LoginIntent.JoinClicked)
            runCurrent()

            assertEquals(listOf<LoginEffect>(LoginEffect.NavigateToJoin), effects)
        }
    }

    @Test
    fun `프로세스가 종료된 뒤 로그인 화면을 복원하면 이메일은 남고 비밀번호는 비어 있어야 한다`() {
        val savedStateHandle = SavedStateHandle()
        val repository = FakeLoginAuthRepository()
        runLoginTest(repository, savedStateHandle) { viewModel ->
            viewModel.onIntent(LoginIntent.EmailChanged("user@example.com"))
            viewModel.onIntent(LoginIntent.PasswordChanged("password"))
        }

        // 프로세스 종료 뒤에는 SavedStateHandle에 저장한 값만 남으므로, 같은 핸들로 ViewModel을 다시 만들어 복원 상황 재현
        val restored = LoginViewModel(repository, savedStateHandle)

        assertEquals("user@example.com", restored.state.value.email)
        assertEquals("", restored.state.value.password)
    }
}

private fun LoginViewModel.signIn(
    email: String,
    password: String,
) {
    onIntent(LoginIntent.EmailChanged(email))
    onIntent(LoginIntent.PasswordChanged(password))
    onIntent(LoginIntent.LoginClicked)
}

private fun runLoginTest(
    repository: FakeLoginAuthRepository,
    savedStateHandle: SavedStateHandle = SavedStateHandle(),
    body: suspend TestScope.(LoginViewModel) -> Unit,
) = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    val viewModel = LoginViewModel(repository, savedStateHandle)
    try {
        body(viewModel)
    } finally {
        viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun TestScope.collectLoginEffects(viewModel: LoginViewModel): List<LoginEffect> {
    val effects = mutableListOf<LoginEffect>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        viewModel.effect.collect { effects += it }
    }
    return effects
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
