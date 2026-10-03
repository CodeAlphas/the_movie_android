package com.codealphas.themovie.presentation.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.auth.AuthRepository
import com.codealphas.themovie.domain.result.Outcome
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JoinViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        // 프로세스 종료 복원용 Bundle에 비밀번호가 남지 않도록, 이메일만 SavedStateHandle에 저장하고 두 비밀번호는 빈 칸으로 복원
        private val _state = MutableStateFlow(JoinUiState(email = savedStateHandle[KEY_EMAIL] ?: ""))
        val state: StateFlow<JoinUiState> = _state.asStateFlow()

        private val _effect = Channel<JoinEffect>(Channel.BUFFERED)
        val effect: Flow<JoinEffect> = _effect.receiveAsFlow()

        fun onIntent(intent: JoinIntent) {
            when (intent) {
                is JoinIntent.EmailChanged -> {
                    savedStateHandle[KEY_EMAIL] = intent.email
                    _state.update { it.copy(email = intent.email) }
                }
                is JoinIntent.PasswordChanged ->
                    _state.update { it.withPasswords(intent.password, it.confirmPassword) }
                is JoinIntent.ConfirmPasswordChanged ->
                    _state.update { it.withPasswords(it.password, intent.confirmPassword) }
                JoinIntent.JoinClicked -> signUp()
                JoinIntent.LoginClicked -> viewModelScope.launch { _effect.send(JoinEffect.NavigateToLogin) }
            }
        }

        private fun JoinUiState.withPasswords(
            password: String,
            confirmPassword: String,
        ): JoinUiState {
            // 비밀번호와 비밀번호 확인이 둘 다 비어 있으면 문자열이 같아 일치 문구가 뜨므로,
            // 비교 문구를 띄우지 않도록 null로 설정
            val passwordsMatch =
                if (password.isEmpty() && confirmPassword.isEmpty()) {
                    null
                } else {
                    password == confirmPassword
                }
            return copy(password = password, confirmPassword = confirmPassword, passwordsMatch = passwordsMatch)
        }

        private fun signUp() {
            val current = _state.value
            if (current.isLoading) return
            val rejection =
                when {
                    current.password != current.confirmPassword -> JoinEffect.ShowPasswordMismatch
                    current.email.isBlank() || current.password.isBlank() -> JoinEffect.ShowInvalidInput
                    // 로그인은 비밀번호 앞뒤 공백을 빼고 요청해 앞뒤 공백이 있는 비밀번호로는 로그인할 수 없으므로,
                    // 그런 비밀번호는 가입 단계에서 거부
                    current.password != current.password.trim() -> JoinEffect.ShowPasswordEdgeWhitespace
                    else -> null
                }
            if (rejection != null) {
                viewModelScope.launch { _effect.send(rejection) }
                return
            }
            _state.update { it.copy(isLoading = true) }
            viewModelScope.launch {
                // 키보드 자동완성이 이메일 끝에 붙인 공백 때문에 Firebase가 형식 오류로 거부하지 않도록 이메일 앞뒤 공백 제거
                val result = authRepository.signUp(current.email.trim(), current.password)
                if (result is Outcome.Success) {
                    // Firebase는 가입에 성공하면 그 계정으로 로그인되므로, 가입 화면에 남은 채 로그인되지 않도록 가입 직후 로그아웃
                    authRepository.signOut()
                }
                _state.update { it.copy(isLoading = false) }
                when (result) {
                    is Outcome.Success -> {
                        _effect.send(JoinEffect.ShowLoginPrompt)
                        _effect.send(JoinEffect.NavigateToLogin)
                    }
                    is Outcome.Failure -> _effect.send(JoinEffect.ShowError(result.error))
                }
            }
        }

        private companion object {
            const val KEY_EMAIL = "email"
        }
    }
