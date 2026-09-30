package com.codealphas.themovie.presentation.auth

import com.codealphas.themovie.domain.auth.AuthError

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
)

sealed interface LoginIntent {
    data class EmailChanged(
        val email: String,
    ) : LoginIntent

    data class PasswordChanged(
        val password: String,
    ) : LoginIntent

    data object LoginClicked : LoginIntent

    data object JoinClicked : LoginIntent
}

sealed interface LoginEffect {
    data object NavigateToMain : LoginEffect

    data object NavigateToJoin : LoginEffect

    data object ShowInvalidInput : LoginEffect

    data class ShowError(
        val error: AuthError,
    ) : LoginEffect
}
