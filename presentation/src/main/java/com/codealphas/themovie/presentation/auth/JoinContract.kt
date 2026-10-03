package com.codealphas.themovie.presentation.auth

import com.codealphas.themovie.domain.auth.AuthError

data class JoinUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val passwordsMatch: Boolean? = null,
    val isLoading: Boolean = false,
)

sealed interface JoinIntent {
    data class EmailChanged(
        val email: String,
    ) : JoinIntent

    data class PasswordChanged(
        val password: String,
    ) : JoinIntent

    data class ConfirmPasswordChanged(
        val confirmPassword: String,
    ) : JoinIntent

    data object JoinClicked : JoinIntent

    data object LoginClicked : JoinIntent
}

sealed interface JoinEffect {
    data object NavigateToLogin : JoinEffect

    data object ShowLoginPrompt : JoinEffect

    data object ShowInvalidInput : JoinEffect

    data object ShowPasswordMismatch : JoinEffect

    data object ShowPasswordEdgeWhitespace : JoinEffect

    data class ShowError(
        val error: AuthError,
    ) : JoinEffect
}
