package com.codealphas.themovie.presentation.review

import com.codealphas.themovie.domain.review.ReviewError
import com.codealphas.themovie.domain.review.ReviewPhoto

// 화면의 별점은 별 5개이고 감상문 점수는 10점 만점이라 두 값의 비율
internal const val RATING_SCALE = 2

data class ReviewEditUiState(
    val title: String = "",
    val content: String = "",
    // 0.0~10.0 점수
    val rating: Double = 0.0,
    val photo: ReviewPhoto = ReviewPhoto.Unchanged,
    // Review.imageUrl은 사진이 없으면 빈 문자열이므로, Unchanged일 때 보여 줄 저장된 사진 URL도 null 대신 빈 문자열 사용
    val savedImageUrl: String = "",
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
) {
    // 화면마다 별점 변환을 따로 계산하지 않도록, 별 5개 기준 값을 상태에서 제공
    val starRating: Float get() = (rating / RATING_SCALE).toFloat()

    // 새로 고른 사진, 저장된 사진, 기본 이미지 중 무엇을 보일지 화면이 따로 판단하지 않도록, 보여 줄 사진 주소를 상태에서 제공
    val shownImage: String?
        get() =
            when (photo) {
                is ReviewPhoto.New -> photo.uri
                ReviewPhoto.Unchanged -> savedImageUrl.ifEmpty { null }
                ReviewPhoto.Removed -> null
            }
}

sealed interface ReviewEditIntent {
    data class TitleChanged(
        val title: String,
    ) : ReviewEditIntent

    data class ContentChanged(
        val content: String,
    ) : ReviewEditIntent

    data class StarRatingChanged(
        val stars: Float,
    ) : ReviewEditIntent

    data class PhotoSelected(
        val uri: String,
    ) : ReviewEditIntent

    data object PhotoRemoved : ReviewEditIntent

    data class CameraStarted(
        val fileUri: String,
    ) : ReviewEditIntent

    data class CameraFinished(
        val isSaved: Boolean,
    ) : ReviewEditIntent

    data object SaveClicked : ReviewEditIntent
}

sealed interface ReviewEditEffect {
    data class Saved(
        val isNew: Boolean,
    ) : ReviewEditEffect

    data object InputRequired : ReviewEditEffect

    data class SaveFailed(
        val error: ReviewError,
    ) : ReviewEditEffect

    data object LoadFailed : ReviewEditEffect
}
