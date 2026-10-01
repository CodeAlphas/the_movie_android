package com.codealphas.themovie.presentation.review

import com.codealphas.themovie.domain.review.Review

data class ReviewListUiState(
    val reviews: List<Review>? = null,
    // ViewModel이 만들어질 때 동기화를 항상 시작하므로, 첫 상태부터 빈 목록 안내 대신 로딩이 보이도록 초깃값을 true로 설정
    val isSyncing: Boolean = true,
)

sealed interface ReviewListIntent {
    data class DeleteClicked(
        val review: Review,
    ) : ReviewListIntent

    data object LogoutClicked : ReviewListIntent
}

sealed interface ReviewListEffect {
    data object ShowSyncFailed : ReviewListEffect

    data object ShowDeleted : ReviewListEffect

    data object NavigateToLogin : ReviewListEffect
}
