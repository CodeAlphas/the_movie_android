package com.codealphas.themovie.domain.review

sealed interface ReviewResult {
    data object Success : ReviewResult

    data class Failure(
        val error: ReviewError,
    ) : ReviewResult
}

enum class ReviewError {
    Unknown,
}
