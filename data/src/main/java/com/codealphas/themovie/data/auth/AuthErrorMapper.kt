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
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

private const val ERROR_INVALID_EMAIL = "ERROR_INVALID_EMAIL"

internal suspend fun <T> toAuthResult(block: suspend () -> T): AuthResult =
    try {
        block()
        Outcome.Success(Unit)
    } catch (_: CancellationException) {
        // Firebase Auth SDK가 요청을 취소해도 취소 예외로 끝나는데 그대로 올리면 로그인이나 가입 중 표시가 풀리지 않으므로,
        // 요청한 코루틴이 취소되지 않았으면 Unknown 실패로 처리
        currentCoroutineContext().ensureActive()
        Outcome.Failure(AuthError.Unknown)
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
            // FirebaseAuthInvalidCredentialsException은 틀린 비밀번호와 잘못된 이메일 형식에 함께 쓰이므로,
            // 예외 타입 대신 errorCode가 ERROR_INVALID_EMAIL인지 보고 이메일 형식 오류만 InvalidEmail로 매핑
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
