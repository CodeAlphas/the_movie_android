package com.codealphas.themovie.domain.auth

import com.codealphas.themovie.domain.result.Outcome

typealias AuthResult = Outcome<Unit, AuthError>

sealed interface AuthError {
    data object InvalidCredentials : AuthError

    data object InvalidEmail : AuthError

    data object EmailAlreadyInUse : AuthError

    data object WeakPassword : AuthError

    data object Network : AuthError

    data object Unknown : AuthError
}
