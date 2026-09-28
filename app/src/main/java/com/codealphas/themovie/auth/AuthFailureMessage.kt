package com.codealphas.themovie.auth

import com.codealphas.themovie.R
import com.codealphas.themovie.domain.auth.AuthError

internal fun loginFailureMessage(error: AuthError): Int =
    when (error) {
        AuthError.InvalidCredentials -> R.string.login_failed
        AuthError.InvalidEmail -> R.string.common_error_invalid_email
        AuthError.Network -> R.string.remote_error_network
        // signInWithEmailAndPassword 문서의 예외에는 가입된 이메일과 6자 미만이 없으므로,
        // 로그인 토스트에 이미 가입한 이메일 문구와 6자 이상 문구가 뜨지 않도록 다시 시도 문구로 안내
        AuthError.EmailAlreadyInUse,
        AuthError.WeakPassword,
        AuthError.Unknown,
        -> R.string.login_failed_unknown
    }

internal fun joinFailureMessage(error: AuthError): Int =
    when (error) {
        AuthError.EmailAlreadyInUse -> R.string.join_failed_exists
        AuthError.InvalidEmail -> R.string.common_error_invalid_email
        AuthError.WeakPassword -> R.string.join_failed_weak_password
        AuthError.Network -> R.string.remote_error_network
        // createUserWithEmailAndPassword 문서의 예외에는 틀린 비밀번호가 없으므로,
        // 회원가입 토스트에 틀린 비밀번호 문구가 뜨지 않도록 다시 시도 문구로 안내
        AuthError.InvalidCredentials,
        AuthError.Unknown,
        -> R.string.join_failed_unknown
    }
