package com.codealphas.themovie.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.auth.AuthError
import com.codealphas.themovie.domain.auth.AuthRepository
import com.codealphas.themovie.domain.auth.AuthResult
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

data class JoinUiState(
    val passwordsMatch: Boolean? = null,
    val isLoading: Boolean = false,
)

sealed interface JoinEvent {
    data object ShowLoginPrompt : JoinEvent

    data object ShowInvalidInput : JoinEvent

    data object ShowPasswordMismatch : JoinEvent

    data class ShowError(
        val error: AuthError,
    ) : JoinEvent
}

@HiltViewModel
class JoinViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(JoinUiState())
        val uiState: StateFlow<JoinUiState> = _uiState.asStateFlow()

        private val _events = Channel<JoinEvent>(Channel.BUFFERED)
        val events: Flow<JoinEvent> = _events.receiveAsFlow()

        fun onPasswordsChanged(
            password: String,
            confirmPassword: String,
        ) {
            // 비밀번호와 비밀번호 확인이 비어 있으면 문자열이 같아 일치 문구가 뜨므로,
            // 비교 문구를 띄우지 않도록 null로 설정
            val passwordsMatch =
                if (password.isEmpty() && confirmPassword.isEmpty()) {
                    null
                } else {
                    password == confirmPassword
                }
            _uiState.update { it.copy(passwordsMatch = passwordsMatch) }
        }

        fun signUp(
            email: String,
            password: String,
            confirmPassword: String,
        ) {
            if (_uiState.value.isLoading) return
            val rejection =
                when {
                    password != confirmPassword -> JoinEvent.ShowPasswordMismatch
                    email.isBlank() || password.isBlank() -> JoinEvent.ShowInvalidInput
                    else -> null
                }
            if (rejection != null) {
                viewModelScope.launch { _events.send(rejection) }
                return
            }
            _uiState.update { it.copy(isLoading = true) }
            viewModelScope.launch {
                val result = authRepository.signUp(email, password)
                if (result is AuthResult.Success) {
                    // Firebase는 가입에 성공하면 그 계정으로 로그인되므로, 가입 화면에 남은 채 로그인되지 않도록 가입 직후 로그아웃
                    authRepository.signOut()
                }
                _uiState.update { it.copy(isLoading = false) }
                val event =
                    when (result) {
                        AuthResult.Success -> JoinEvent.ShowLoginPrompt
                        is AuthResult.Failure -> JoinEvent.ShowError(result.error)
                    }
                _events.send(event)
            }
        }
    }
