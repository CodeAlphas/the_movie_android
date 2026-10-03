package com.codealphas.themovie.domain.review

data class ReviewDraft(
    val id: String?,
    val title: String,
    val content: String,
    val rating: Double,
    val photo: ReviewPhoto,
)

sealed interface ReviewPhoto {
    data object Unchanged : ReviewPhoto

    data object Removed : ReviewPhoto

    // :domain은 Android 의존성이 없어 Uri 타입을 쓸 수 없으므로, 사진 주소를 문자열로 전달
    data class New(
        val uri: String,
    ) : ReviewPhoto
}
