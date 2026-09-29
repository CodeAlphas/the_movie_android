package com.codealphas.themovie.domain.review

data class ReviewDraft(
    val id: Int?,
    val title: String,
    val content: String,
    val rating: Double,
    val photo: ReviewPhoto,
)

sealed interface ReviewPhoto {
    data object Unchanged : ReviewPhoto

    data object Removed : ReviewPhoto

    // :domain은 Android Uri를 모르므로 문자열로 전달
    data class New(
        val uri: String,
    ) : ReviewPhoto
}
