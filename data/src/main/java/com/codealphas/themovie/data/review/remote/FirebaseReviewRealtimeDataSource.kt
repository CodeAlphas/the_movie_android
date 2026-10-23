package com.codealphas.themovie.data.review.remote

import com.codealphas.themovie.data.review.toRemoteFields
import com.codealphas.themovie.data.review.toReviewOrNull
import com.codealphas.themovie.domain.review.Review
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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

        // Task로 기다리면 실패가 오류 코드 없이 메시지만 담은 DatabaseException으로 와 권한 거부와 네트워크 오류를 구분할 수 없으므로,
        // 오류 코드가 든 DatabaseError를 받도록 CompletionListener로 쓰기 완료 대기
        override suspend fun save(
            userId: String,
            review: Review,
        ) {
            suspendCancellableCoroutine { continuation ->
                reviewsReference(userId).child(review.id).updateChildren(review.toRemoteFields()) { error, _ ->
                    if (error == null) {
                        continuation.resume(Unit)
                    } else {
                        continuation.resumeWithException(error.toReviewRemoteException())
                    }
                }
            }
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

private fun DataSnapshot.toReviewOrNull(): Review? =
    key?.let { id -> toReviewOrNull(id = id, fields = value as? Map<*, *>) }

// 보안 규칙이 거부한 쓰기는 다시 보내도 같은 결과라 재시도하면 업로드가 끝나지 않으므로, PERMISSION_DENIED만 권한 거부로 변환
private fun DatabaseError.toReviewRemoteException(): Exception =
    if (code == DatabaseError.PERMISSION_DENIED) ReviewPermissionDeniedException(toException()) else toException()
