package com.codealphas.themovie.presentation.review

import com.codealphas.themovie.domain.review.Review

data class ReviewListUiState(
    val reviews: List<Review>? = null,
    // ViewModel이 만들어질 때 동기화를 항상 시작하므로, 첫 상태부터 빈 목록 안내 대신 로딩이 보이도록 초깃값을 true로 설정
    val isSyncing: Boolean = true,
    // 로그아웃 확인 창은 서버에 반영하지 않은 감상문 변경이 있을 때만 띄우므로, 창에 보일 변경 수를 담고 창을 띄우지 않을 때는 null 적용
    val logoutPendingReviewCount: Int? = null,
)

sealed interface ReviewListIntent {
    data class DeleteClicked(
        val review: Review,
    ) : ReviewListIntent

    data object LogoutClicked : ReviewListIntent

    data object LogoutConfirmed : ReviewListIntent

    data object LogoutDismissed : ReviewListIntent
}

sealed interface ReviewListEffect {
    data object ShowSyncFailed : ReviewListEffect

    data object ShowDeleted : ReviewListEffect

    data object ShowDeleteFailed : ReviewListEffect

    data object NavigateToLogin : ReviewListEffect
}
