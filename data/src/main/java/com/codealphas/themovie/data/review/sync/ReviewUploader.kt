package com.codealphas.themovie.data.review.sync

import com.codealphas.themovie.data.review.local.ReviewDao
import com.codealphas.themovie.data.review.local.ReviewEntity
import com.codealphas.themovie.data.review.local.ReviewSyncState
import com.codealphas.themovie.data.review.remote.ReviewPermissionDeniedException
import com.codealphas.themovie.data.review.remote.ReviewRealtimeDataSource
import com.codealphas.themovie.data.review.remote.ReviewStorageDataSource
import com.codealphas.themovie.data.review.toReview
import com.codealphas.themovie.domain.auth.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

// 네트워크가 연결됐는데 Firebase 서버가 응답하지 않으면 쓰기와 삭제가 끝나지 않아 WorkManager가 실행 한도인 10분이 지나야 작업을 멈추므로,
// 그 전에 재시도로 넘어가도록 서버 응답 대기 시간 제한
internal val SERVER_WRITE_TIMEOUT = 30.seconds

internal class ReviewUploader
    @Inject
    constructor(
        private val reviewDao: ReviewDao,
        private val realtimeDataSource: ReviewRealtimeDataSource,
        private val storageDataSource: ReviewStorageDataSource,
        private val authRepository: AuthRepository,
        private val syncLock: ReviewSyncLock,
    ) {
        suspend fun upload(): ReviewUploadResult {
            // 로그아웃하면 Room 감상문도 함께 지우므로, 올릴 감상문이 없는 상태로 보고 성공 처리
            val userId = authRepository.currentUserId() ?: return ReviewUploadResult.SUCCESS
            return uploadPending(userId)
        }

        private suspend fun uploadPending(userId: String): ReviewUploadResult {
            var permissionDenied = false
            for (review in reviewDao.getPending()) {
                val result =
                    when (review.syncState) {
                        ReviewSyncState.PENDING_UPSERT -> write(userId, review)
                        ReviewSyncState.PENDING_DELETE -> remove(userId, review)
                        // getPending()은 SYNCED 감상문을 돌려주지 않으므로, 올릴 것이 없는 감상문으로 보고 성공 처리
                        ReviewSyncState.SYNCED -> ReviewUploadResult.SUCCESS
                    }
                when (result) {
                    ReviewUploadResult.SUCCESS -> Unit
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
        ): ReviewUploadResult {
            var result = requestServer { realtimeDataSource.save(userId, review.toReview()) }
            // 감상문 업로드가 실패했는데 이전 사진을 지우면 서버에 남은 예전 감상문이 지운 사진을 가리키므로, 감상문 업로드를 확인한 뒤에만 삭제
            if (result == ReviewUploadResult.SUCCESS) result = deletePhoto(review.storageFileNameToDelete)
            if (result == ReviewUploadResult.SUCCESS) {
                // 사진 삭제가 실패해도 다음 실행에서 다시 지울 수 있도록 Storage에서 사진을 지운 뒤에 storageFileNameToDelete를 빈 문자열로 변경
                if (review.storageFileNameToDelete.isNotEmpty()) {
                    reviewDao.clearStorageFileNameToDelete(review.id, review.storageFileNameToDelete)
                }
                reviewDao.markSynced(review.id, review.localVersion)
            }
            return result
        }

        // 동기화가 지운 감상문을 Room에 다시 넣지 않도록 서버 삭제부터 행 삭제까지 동기화와 같은 잠금 적용
        private suspend fun remove(
            userId: String,
            review: ReviewEntity,
        ): ReviewUploadResult = syncLock.withLock { removeFromServerAndRoom(userId, review) }

        private suspend fun removeFromServerAndRoom(
            userId: String,
            review: ReviewEntity,
        ): ReviewUploadResult {
            var result = requestServer { realtimeDataSource.delete(userId, review.id) }
            // 서버 삭제가 실패했는데 사진을 지우면 서버에 남은 감상문이 로그아웃 뒤 다시 로그인하거나 다른 기기에서 받을 때
            // 깨진 사진으로 나타나므로, 서버 삭제를 확인한 뒤에만 사진 삭제
            if (result == ReviewUploadResult.SUCCESS) result = deletePhoto(review.storageFileName)
            // 감상문 업로드 전에 사진을 바꾸거나 지운 뒤 감상문을 지우면 이전 사진 파일도 Storage에 남아 있으므로, 이전 사진 파일도 삭제
            if (result == ReviewUploadResult.SUCCESS) result = deletePhoto(review.storageFileNameToDelete)
            // 사진 삭제가 실패해도 다음 실행에서 행에 남은 파일 이름으로 다시 지울 수 있도록 사진을 지운 뒤에 행 삭제
            if (result == ReviewUploadResult.SUCCESS) reviewDao.deleteIfPendingDelete(review.id)
            return result
        }

        private suspend fun deletePhoto(fileName: String): ReviewUploadResult {
            if (fileName.isEmpty()) return ReviewUploadResult.SUCCESS
            // Storage가 응답하지 않아도 감상문 삭제가 동기화 잠금을 오래 잡지 않도록, 감상문 서버 쓰기와 같은 응답 대기 시간 제한 적용
            val result = requestServer { storageDataSource.delete(fileName) }
            // 보안 규칙이 삭제를 거부한 사진은 다시 지워도 같은 결과인데 행을 남기면 서버에서 이미 지운 감상문이 대기 상태로 계속 남으므로,
            // 사진 파일이 Storage에 남는 것을 허용하고 행 삭제로 진행
            return if (result == ReviewUploadResult.PERMANENT_FAILURE) ReviewUploadResult.SUCCESS else result
        }

        private suspend fun requestServer(block: suspend () -> Unit): ReviewUploadResult =
            request {
                val finished = withTimeoutOrNull(SERVER_WRITE_TIMEOUT) { block() }
                if (finished == null) ReviewUploadResult.RETRY else ReviewUploadResult.SUCCESS
            }

        private suspend fun request(block: suspend () -> ReviewUploadResult): ReviewUploadResult =
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (_: ReviewPermissionDeniedException) {
                ReviewUploadResult.PERMANENT_FAILURE
            } catch (_: Exception) {
                ReviewUploadResult.RETRY
            }
    }

internal enum class ReviewUploadResult {
    /** 대기 중인 저장과 삭제를 모두 서버에 반영했다. 업로드하는 동안 다시 수정되거나 삭제된 감상문은 대기 상태로 남는다. */
    SUCCESS,

    /** 네트워크 오류나 시간 초과로 멈췄다. 남은 감상문은 대기 상태로 남는다. */
    RETRY,

    /** 서버가 거부한 감상문을 건너뛰고 나머지를 반영했다. 거부된 감상문은 대기 상태로 남는다. */
    PERMANENT_FAILURE,
}
