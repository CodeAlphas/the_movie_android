package com.codealphas.themovie.data.review.sync

import com.codealphas.themovie.data.review.local.ReviewDao
import com.codealphas.themovie.data.review.local.ReviewEntity
import com.codealphas.themovie.data.review.local.ReviewSyncState
import com.codealphas.themovie.data.review.remote.ReviewPermissionDeniedException
import com.codealphas.themovie.data.review.remote.ReviewRealtimeDataSource
import com.codealphas.themovie.data.review.toReview
import com.codealphas.themovie.domain.auth.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

// 네트워크가 연결됐는데 Firebase 서버가 응답하지 않으면 쓰기가 끝나지 않아 WorkManager가 실행 한도인 10분이 지나야 작업을 멈추므로,
// 그 전에 재시도로 넘어가도록 서버 쓰기 대기 시간 제한
internal val SERVER_WRITE_TIMEOUT = 30.seconds

internal class ReviewUploader
    @Inject
    constructor(
        private val reviewDao: ReviewDao,
        private val realtimeDataSource: ReviewRealtimeDataSource,
        private val authRepository: AuthRepository,
    ) {
        suspend fun upload(): ReviewUploadResult {
            // 로그아웃하면 Room 감상문도 함께 지우므로, 올릴 감상문이 없는 상태로 보고 성공 처리
            val userId = authRepository.currentUserId() ?: return ReviewUploadResult.SUCCESS
            return uploadPending(userId)
        }

        private suspend fun uploadPending(userId: String): ReviewUploadResult {
            var permissionDenied = false
            val pending = reviewDao.getPending().filter { it.syncState == ReviewSyncState.PENDING_UPSERT }
            for (review in pending) {
                when (write(userId, review)) {
                    ReviewUploadResult.SUCCESS -> reviewDao.markSynced(review.id, review.localVersion)
                    // 연결 문제는 다음 감상문도 같은 이유로 실패하므로, 남은 감상문을 시도하지 않고 재시도로 종료
                    ReviewUploadResult.RETRY -> return ReviewUploadResult.RETRY
                    // 감상문 하나의 권한 거부가 다른 감상문의 업로드를 막지 않도록, 거부된 감상문만 건너뛰고 계속 업로드
                    ReviewUploadResult.PERMANENT_FAILURE -> permissionDenied = true
                }
            }
            return if (permissionDenied) ReviewUploadResult.PERMANENT_FAILURE else ReviewUploadResult.SUCCESS
        }

        private suspend fun write(
            userId: String,
            review: ReviewEntity,
        ): ReviewUploadResult =
            try {
                val written =
                    withTimeoutOrNull(SERVER_WRITE_TIMEOUT) {
                        realtimeDataSource.save(userId, review.toReview())
                    }
                if (written == null) ReviewUploadResult.RETRY else ReviewUploadResult.SUCCESS
            } catch (e: CancellationException) {
                throw e
            } catch (_: ReviewPermissionDeniedException) {
                ReviewUploadResult.PERMANENT_FAILURE
            } catch (_: Exception) {
                ReviewUploadResult.RETRY
            }
    }

internal enum class ReviewUploadResult {
    /** 대기 중인 감상문을 모두 올렸다. 업로드하는 동안 다시 수정된 감상문은 대기 상태로 남는다. */
    SUCCESS,

    /** 네트워크 오류나 시간 초과로 멈췄다. 남은 감상문은 대기 상태로 남는다. */
    RETRY,

    /** 서버가 거부한 감상문을 건너뛰고 나머지를 올렸다. 거부된 감상문은 대기 상태로 남는다. */
    PERMANENT_FAILURE,
}
