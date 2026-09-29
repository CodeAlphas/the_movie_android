package com.codealphas.themovie.data.review.remote

import com.codealphas.themovie.domain.review.Review
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class FirebaseReviewRealtimeDataSource
    @Inject
    constructor(
        private val database: FirebaseDatabase,
    ) : ReviewRealtimeDataSource {
        // 오프라인에서 캐시가 없으면 리스너는 연결될 때까지 결과를 주지 않으므로, 실패로 끝나는 get()으로 읽음
        override suspend fun getAll(userId: String): List<Review> =
            reviewsReference(userId)
                .get()
                .await()
                .children
                .mapNotNull(DataSnapshot::toReviewOrNull)

        // 오프라인이면 삭제가 연결될 때까지 완료되지 않아 화면이 멈추므로, 완료를 기다리지 않고 SDK 대기열에 맡김
        override fun delete(
            userId: String,
            reviewId: Int,
        ) {
            reviewsReference(userId).child(reviewId.toString()).removeValue()
        }

        private fun reviewsReference(userId: String): DatabaseReference =
            database.reference
                .child("users")
                .child(userId)
                .child("reviews")
    }

// 필드가 빠진 항목 하나가 예외를 던지면 목록 전체 동기화가 중단되므로, 읽지 못한 항목만 제외
private fun DataSnapshot.toReviewOrNull(): Review? {
    val id = key?.toIntOrNull()
    val rating = child("rating").value?.toString()?.toDoubleOrNull()
    if (id == null || rating == null) return null
    return Review(
        title = child("title").value.toString(),
        image = child("image").value.toString(),
        content = child("content").value.toString(),
        time = child("time").value.toString(),
        rating = rating,
        storageFileName = child("storageFileName").value.toString(),
        id = id,
    )
}
