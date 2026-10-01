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
class LoginViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        // 프로세스 종료 복원용 Bundle에 비밀번호가 남지 않도록, 이메일만 SavedStateHandle에 저장하고 비밀번호는 빈 칸으로 복원
        private val _state = MutableStateFlow(LoginUiState(email = savedStateHandle[KEY_EMAIL] ?: ""))
        val state: StateFlow<LoginUiState> = _state.asStateFlow()

        private val _effect = Channel<LoginEffect>(Channel.BUFFERED)
        val effect: Flow<LoginEffect> = _effect.receiveAsFlow()

        fun onIntent(intent: LoginIntent) {
            when (intent) {
                is LoginIntent.EmailChanged -> {
                    savedStateHandle[KEY_EMAIL] = intent.email
                    _state.update { it.copy(email = intent.email) }
                }
                is LoginIntent.PasswordChanged -> _state.update { it.copy(password = intent.password) }
                LoginIntent.LoginClicked -> signIn()
                LoginIntent.JoinClicked -> viewModelScope.launch { _effect.send(LoginEffect.NavigateToJoin) }
            }
        }

        private fun signIn() {
            val current = _state.value
            if (current.isLoading) return
            if (current.email.isBlank() || current.password.isBlank()) {
                viewModelScope.launch { _effect.send(LoginEffect.ShowInvalidInput) }
                return
            }
            _state.update { it.copy(isLoading = true) }
            viewModelScope.launch {
                val effect =
                    when (val result = authRepository.signIn(current.email, current.password)) {
                        is Outcome.Success -> LoginEffect.NavigateToHome
                        is Outcome.Failure -> LoginEffect.ShowError(result.error)
                    }
                _state.update { it.copy(isLoading = false) }
                _effect.send(effect)
            }
        }

        private companion object {
            const val KEY_EMAIL = "email"
        }
    }
