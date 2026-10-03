package com.codealphas.themovie.data.review.remote

import com.codealphas.themovie.domain.review.Review

internal interface ReviewRealtimeDataSource {
    suspend fun getAll(userId: String): List<Review>

    /**
     * 새 감상문의 id로 쓸 서버 키를 네트워크 없이 기기에서 만든다.
     * 키는 기기끼리 겹치지 않고, 문자열 순서가 만든 순서와 같다.
     */
    fun newKey(userId: String): String

    /**
     * 감상문을 서버에 쓰도록 요청만 하고 완료를 기다리지 않는다.
     * 요청을 보내지 못하면 예외를 던지지만, 서버 쓰기가 나중에 실패하면 알리지 않는다.
     */
    fun save(
        userId: String,
        review: Review,
    )

    /**
     * 감상문을 서버에서 지우도록 요청만 하고 완료를 기다리지 않는다.
     * 요청을 보내지 못하면 예외를 던지지만, 서버 삭제가 나중에 실패하면 알리지 않는다.
     */
    fun delete(
        userId: String,
        reviewId: String,
    )
}
