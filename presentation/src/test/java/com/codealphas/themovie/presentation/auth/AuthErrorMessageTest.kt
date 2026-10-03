package com.codealphas.themovie.presentation.auth

import com.codealphas.themovie.domain.auth.AuthError
import com.codealphas.themovie.presentation.R
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthErrorMessageTest {
    @Test
    fun `로그인 실패는 이유별 문구로 바뀌고 로그인에서 나올 수 없는 실패는 Unknown 문구로 바뀌어야 한다`() {
        val expected =
            mapOf(
                AuthError.InvalidCredentials to R.string.login_failed,
                AuthError.InvalidEmail to R.string.common_error_invalid_email,
                AuthError.Network to R.string.remote_error_network,
                AuthError.EmailAlreadyInUse to R.string.login_failed_unknown,
                AuthError.WeakPassword to R.string.login_failed_unknown,
                AuthError.Unknown to R.string.login_failed_unknown,
            )

        assertEquals(expected, expected.keys.associateWith(::loginErrorMessage))
    }

    @Test
    fun `회원가입 실패는 이유별 문구로 바뀌고 가입에서 나올 수 없는 실패는 Unknown 문구로 바뀌어야 한다`() {
        val expected =
            mapOf(
                AuthError.EmailAlreadyInUse to R.string.join_failed_exists,
                AuthError.InvalidEmail to R.string.common_error_invalid_email,
                AuthError.WeakPassword to R.string.join_failed_weak_password,
                AuthError.Network to R.string.remote_error_network,
                AuthError.InvalidCredentials to R.string.join_failed_unknown,
                AuthError.Unknown to R.string.join_failed_unknown,
            )

        assertEquals(expected, expected.keys.associateWith(::joinErrorMessage))
    }
}
