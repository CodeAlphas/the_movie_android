package com.codealphas.themovie.domain.result

sealed interface Outcome<out T, out E> {
    data class Success<T>(
        val data: T,
    ) : Outcome<T, Nothing>

    data class Failure<E>(
        val error: E,
    ) : Outcome<Nothing, E>
}

fun <T, E, R> Outcome<T, E>.map(transform: (T) -> R): Outcome<R, E> =
    when (this) {
        is Outcome.Success -> Outcome.Success(transform(data))
        is Outcome.Failure -> this
    }

// ViewModel이 분기 안에서 Channel.send 같은 suspend 함수를 부르므로, 람다에서 suspend 호출이 되도록 inline 적용
inline fun <T, E> Outcome<T, E>.onSuccess(action: (T) -> Unit): Outcome<T, E> {
    if (this is Outcome.Success) action(data)
    return this
}

inline fun <T, E> Outcome<T, E>.onFailure(action: (E) -> Unit): Outcome<T, E> {
    if (this is Outcome.Failure) action(error)
    return this
}
