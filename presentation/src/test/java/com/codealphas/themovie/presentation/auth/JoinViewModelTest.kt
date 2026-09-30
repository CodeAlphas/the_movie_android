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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JoinViewModelTest {
    @Test
    fun `두 비밀번호가 같으면 passwordsMatch가 true여야 한다`() {
        runJoinTest(FakeJoinAuthRepository()) { viewModel ->
            viewModel.changePasswords("secret", "secret")

            assertEquals(true, viewModel.state.value.passwordsMatch)
        }
    }

    @Test
    fun `두 비밀번호가 다르면 passwordsMatch가 false여야 한다`() {
        runJoinTest(FakeJoinAuthRepository()) { viewModel ->
            viewModel.changePasswords("secret", "secre")

            assertEquals(false, viewModel.state.value.passwordsMatch)
        }
    }

    @Test
    fun `두 비밀번호가 모두 비면 passwordsMatch가 null이어야 한다`() {
        runJoinTest(FakeJoinAuthRepository()) { viewModel ->
            viewModel.changePasswords("secret", "secret")
            viewModel.changePasswords("", "")
            assertNull(viewModel.state.value.passwordsMatch)
        }
    }

    @Test
    fun `이메일과 상관없이 비밀번호가 다르면 불일치 안내를 보내야 한다`() {
        val repository = FakeJoinAuthRepository()
        runJoinTest(repository) { viewModel ->
            val effects = collectJoinEffects(viewModel)

            viewModel.signUp("", "secret", "")
            runCurrent()

            assertEquals(listOf<JoinEffect>(JoinEffect.ShowPasswordMismatch), effects)
            assertEquals(emptyList<String>(), repository.calls)
        }
    }

    @Test
    fun `이메일이나 비밀번호가 비어 있거나 공백뿐이면 입력 안내를 보내야 한다`() {
        val repository = FakeJoinAuthRepository()
        runJoinTest(repository) { viewModel ->
            val effects = collectJoinEffects(viewModel)

            viewModel.signUp(" ", "secret", "secret")
            viewModel.signUp("user@example.com", "", "")
            runCurrent()

            assertEquals(listOf(JoinEffect.ShowInvalidInput, JoinEffect.ShowInvalidInput), effects)
            assertEquals(emptyList<String>(), repository.calls)
            assertFalse(viewModel.state.value.isLoading)
        }
    }

    @Test
    fun `가입 중에 가입 버튼을 다시 누르면 가입을 한 번만 요청해야 한다`() {
        val result = CompletableDeferred<AuthResult>()
        val repository = FakeJoinAuthRepository(signUpResult = result)
        runJoinTest(repository) { viewModel ->
            viewModel.signUp("user@example.com", "secret", "secret")
            runCurrent()
            viewModel.signUp("user@example.com", "secret", "secret")
            runCurrent()

            assertEquals(listOf("signUp"), repository.calls)
            assertTrue(viewModel.state.value.isLoading)
            result.complete(Outcome.Success(Unit))
        }
    }

    @Test
    fun `가입에 성공하면 로그아웃한 뒤 로딩을 끝내고 로그인 안내를 보내야 한다`() {
        val repository = FakeJoinAuthRepository(signUpResult = CompletableDeferred(Outcome.Success(Unit)))
        runJoinTest(repository) { viewModel ->
            val effects = collectJoinEffects(viewModel)

            viewModel.signUp("user@example.com", "secret", "secret")
            runCurrent()

            assertEquals(listOf("signUp", "signOut"), repository.calls)
            assertEquals(listOf<JoinEffect>(JoinEffect.ShowLoginPrompt), effects)
            assertFalse(viewModel.state.value.isLoading)
        }
    }

    @Test
    fun `가입에 실패하면 로그아웃하지 않고 로딩을 끝내고 실패 이유를 보내야 한다`() {
        val failure = Outcome.Failure(AuthError.EmailAlreadyInUse)
        val repository = FakeJoinAuthRepository(signUpResult = CompletableDeferred(failure))
        runJoinTest(repository) { viewModel ->
            val effects = collectJoinEffects(viewModel)

            viewModel.signUp("user@example.com", "secret", "secret")
            runCurrent()

            assertEquals(listOf("signUp"), repository.calls)
            assertEquals(listOf<JoinEffect>(JoinEffect.ShowError(AuthError.EmailAlreadyInUse)), effects)
            assertFalse(viewModel.state.value.isLoading)
        }
    }

    @Test
    fun `로그인하기를 누르면 로그인 화면 이동 이벤트를 보내야 한다`() {
        runJoinTest(FakeJoinAuthRepository()) { viewModel ->
            val effects = collectJoinEffects(viewModel)

            viewModel.onIntent(JoinIntent.LoginClicked)
            runCurrent()

            assertEquals(listOf<JoinEffect>(JoinEffect.NavigateToLogin), effects)
        }
    }

    @Test
    fun `프로세스가 종료된 뒤 회원가입 화면을 복원하면 이메일은 남고 두 비밀번호는 비어 있어야 한다`() {
        val savedStateHandle = SavedStateHandle()
        val repository = FakeJoinAuthRepository()
        runJoinTest(repository, savedStateHandle) { viewModel ->
            viewModel.onIntent(JoinIntent.EmailChanged("user@example.com"))
            viewModel.changePasswords("secret", "secret")
        }

        // 프로세스 종료 뒤에는 SavedStateHandle에 저장한 값만 남으므로, 같은 핸들로 ViewModel을 다시 만들어 복원 상황 재현
        val restored = JoinViewModel(repository, savedStateHandle)

        assertEquals("user@example.com", restored.state.value.email)
        assertEquals("", restored.state.value.password)
        assertEquals("", restored.state.value.confirmPassword)
        assertNull(restored.state.value.passwordsMatch)
    }
}

private fun JoinViewModel.changePasswords(
    password: String,
    confirmPassword: String,
) {
    onIntent(JoinIntent.PasswordChanged(password))
    onIntent(JoinIntent.ConfirmPasswordChanged(confirmPassword))
}

private fun JoinViewModel.signUp(
    email: String,
    password: String,
    confirmPassword: String,
) {
    onIntent(JoinIntent.EmailChanged(email))
    changePasswords(password, confirmPassword)
    onIntent(JoinIntent.JoinClicked)
}

private fun runJoinTest(
    repository: FakeJoinAuthRepository,
    savedStateHandle: SavedStateHandle = SavedStateHandle(),
    body: suspend TestScope.(JoinViewModel) -> Unit,
) = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    val viewModel = JoinViewModel(repository, savedStateHandle)
    try {
        body(viewModel)
    } finally {
        viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun TestScope.collectJoinEffects(viewModel: JoinViewModel): List<JoinEffect> {
    val effects = mutableListOf<JoinEffect>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        viewModel.effect.collect { effects += it }
    }
    return effects
}

private class FakeJoinAuthRepository(
    private val signUpResult: CompletableDeferred<AuthResult> = CompletableDeferred(),
) : AuthRepository {
    val calls = mutableListOf<String>()

    override suspend fun signIn(
        email: String,
        password: String,
    ): AuthResult = error("사용하지 않음")

    override suspend fun signUp(
        email: String,
        password: String,
    ): AuthResult {
        calls += "signUp"
        return signUpResult.await()
    }

    override fun currentUserId(): String? = error("사용하지 않음")

    override fun signOut() {
        calls += "signOut"
    }
}
