package com.codealphas.themovie.data.review

import javax.inject.Inject

// 테스트에서 저장 시각과 사진 파일 이름을 고정할 수 있도록 현재 시각을 인터페이스로 분리
internal fun interface ReviewClock {
    fun nowMillis(): Long
}

internal class SystemReviewClock
    @Inject
    constructor() : ReviewClock {
        override fun nowMillis(): Long = System.currentTimeMillis()
    }
