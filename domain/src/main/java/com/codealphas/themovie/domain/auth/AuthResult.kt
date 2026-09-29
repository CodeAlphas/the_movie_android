package com.codealphas.themovie.domain.auth

sealed interface AuthResult {
    data object Success : AuthResult

    data class Failure(
        val error: AuthError,
    ) : AuthResult
}

sealed interface AuthError {
    data object InvalidCredentials : AuthError

    data object InvalidEmail : AuthError

    data object EmailAlreadyInUse : AuthError

    data object WeakPassword : AuthError

    data object Network : AuthError

    data object Unknown : AuthError
}
