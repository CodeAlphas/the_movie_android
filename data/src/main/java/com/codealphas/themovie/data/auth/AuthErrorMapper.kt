package com.codealphas.themovie.data.auth

import com.codealphas.themovie.domain.auth.AuthError
import com.codealphas.themovie.domain.auth.AuthResult
import com.codealphas.themovie.domain.result.Outcome
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.CancellationException

private const val ERROR_INVALID_EMAIL = "ERROR_INVALID_EMAIL"

internal suspend fun <T> toAuthResult(block: suspend () -> T): AuthResult =
    try {
        block()
        Outcome.Success(Unit)
    } catch (cancellation: CancellationException) {
        // 취소를 실패로 바꾸면 화면이 사라진 뒤에도 로그인이나 가입 안내가 나가므로, 호출한 코루틴이 멈추도록 취소 예외를 다시 던짐
        throw cancellation
    } catch (error: FirebaseAuthException) {
        Outcome.Failure(error.toAuthError())
    } catch (error: FirebaseNetworkException) {
        Outcome.Failure(error.toAuthError())
    } catch (_: Exception) {
        Outcome.Failure(AuthError.Unknown)
    }

internal fun Throwable.toAuthError(): AuthError =
    when (this) {
        // FirebaseAuthWeakPasswordException은 FirebaseAuthInvalidCredentialsException의 하위 타입이므로,
        // 약한 비밀번호가 자격 증명 오류로 바뀌지 않도록 먼저 매핑
        is FirebaseAuthWeakPasswordException -> AuthError.WeakPassword
        is FirebaseAuthInvalidCredentialsException ->
            if (errorCode == ERROR_INVALID_EMAIL) {
                AuthError.InvalidEmail
            } else {
                AuthError.InvalidCredentials
            }
        // 이메일 열거 보호가 켜진 프로젝트는 없는 계정과 틀린 비밀번호를 구분하지 않으므로, 없는 사용자 예외도 InvalidCredentials로 매핑
        is FirebaseAuthInvalidUserException -> AuthError.InvalidCredentials
        is FirebaseAuthUserCollisionException -> AuthError.EmailAlreadyInUse
        is FirebaseNetworkException -> AuthError.Network
        else -> AuthError.Unknown
    }
