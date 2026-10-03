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
        // 오프라인에서 메모리 캐시가 없으면 리스너는 연결될 때까지 결과를 주지 않아 동기화가 끝나지 않으므로,
        // 이때 기다리지 않고 실패를 돌려주는 get() 사용
        override suspend fun getAll(userId: String): List<Review> =
            reviewsReference(userId)
                .get()
                .await()
                .children
                .mapNotNull(DataSnapshot::toReviewOrNull)

        // push 키가 null인 경우는 루트 경로에서 push()할 때뿐이라 사용자 감상문 경로에서는 없으므로, null이면 예외 처리
        override fun newKey(userId: String): String =
            checkNotNull(reviewsReference(userId).push().key) { "감상문 서버 키를 만들지 못함" }

        // 오프라인이면 쓰기가 연결될 때까지 완료되지 않아 화면이 멈추므로, 완료를 기다리지 않고 SDK 대기열에 위임
        // SDK 대기열은 메모리에만 있어 연결 전에 앱이 종료되면 서버 쓰기가 빠지므로, 그 감상문이 이 기기에만 남는 한계를 허용
        override fun save(
            userId: String,
            review: Review,
        ) {
            val fields =
                mapOf(
                    "image" to review.imageUrl,
                    "title" to review.title,
                    "content" to review.content,
                    "time" to review.time,
                    "rating" to review.rating,
                    "storageFileName" to review.storageFileName,
                )
            reviewsReference(userId).child(review.id).updateChildren(fields)
        }

        // 오프라인이면 삭제가 연결될 때까지 완료되지 않아 화면이 멈추므로, 완료를 기다리지 않고 SDK 대기열에 위임
        // SDK 대기열은 메모리에만 있어 연결 전에 앱이 종료되면 서버 삭제가 빠지므로,
        // 다음 동기화 때 삭제한 감상문이 사진 링크가 깨진 채 되살아나는 한계를 허용
        override fun delete(
            userId: String,
            reviewId: String,
        ) {
            reviewsReference(userId).child(reviewId).removeValue()
        }

        private fun reviewsReference(userId: String): DatabaseReference =
            database.reference
                .child("users")
                .child(userId)
                .child("reviews")
    }

// 필드가 빠진 항목 하나가 예외를 던지면 목록 전체 동기화가 중단되므로, 읽지 못한 항목만 제외
private fun DataSnapshot.toReviewOrNull(): Review? {
    val id = key
    val rating = child("rating").value?.toString()?.toDoubleOrNull()
    if (id == null || rating == null) return null
    return Review(
        title = child("title").value.toString(),
        imageUrl = child("image").value.toString(),
        content = child("content").value.toString(),
        time = child("time").value.toString(),
        rating = rating,
        storageFileName = child("storageFileName").value.toString(),
        id = id,
    )
}
