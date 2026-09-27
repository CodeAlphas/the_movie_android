package com.codealphas.themovie.domain

sealed interface DataResult<out T> {
    data class Success<T>(
        val data: T,
    ) : DataResult<T>

    data class Failure(
        val error: RemoteError,
    ) : DataResult<Nothing>
}
