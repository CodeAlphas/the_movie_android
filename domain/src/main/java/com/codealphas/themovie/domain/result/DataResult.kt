package com.codealphas.themovie.domain.result

sealed interface DataResult<out T> {
    data class Success<T>(
        val data: T,
    ) : DataResult<T>

    data class Failure(
        val error: RemoteError,
    ) : DataResult<Nothing>
}

fun <T, R> DataResult<T>.map(transform: (T) -> R): DataResult<R> =
    when (this) {
        is DataResult.Success -> DataResult.Success(transform(data))
        is DataResult.Failure -> this
    }
