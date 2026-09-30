package com.codealphas.themovie.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.auth.AuthError
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

data class LoginUiState(
    val isLoading: Boolean = false,
)

sealed interface LoginEvent {
    data object NavigateToMain : LoginEvent

    data object ShowInvalidInput : LoginEvent

    data class ShowError(
        val error: AuthError,
    ) : LoginEvent
}

@HiltViewModel
class LoginViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(LoginUiState())
        val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

        private val _events = Channel<LoginEvent>(Channel.BUFFERED)
        val events: Flow<LoginEvent> = _events.receiveAsFlow()

        fun signIn(
            email: String,
            password: String,
        ) {
            if (_uiState.value.isLoading) return
            if (email.isBlank() || password.isBlank()) {
                viewModelScope.launch { _events.send(LoginEvent.ShowInvalidInput) }
                return
            }
            _uiState.update { it.copy(isLoading = true) }
            viewModelScope.launch {
                val event =
                    when (val result = authRepository.signIn(email, password)) {
                        is Outcome.Success -> LoginEvent.NavigateToMain
                        is Outcome.Failure -> LoginEvent.ShowError(result.error)
                    }
                _uiState.update { it.copy(isLoading = false) }
                _events.send(event)
            }
        }
    }
