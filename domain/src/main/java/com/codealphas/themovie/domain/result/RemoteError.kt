package com.codealphas.themovie.domain.result

sealed interface RemoteError {
    data object Timeout : RemoteError

    data object Network : RemoteError

    data class Http(
        val code: Int,
    ) : RemoteError

    data object Serialization : RemoteError

    data object Unknown : RemoteError
}
