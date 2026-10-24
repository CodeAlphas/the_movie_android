package com.codealphas.themovie.data.review

import com.codealphas.themovie.data.review.local.ReviewDao
import com.codealphas.themovie.data.review.local.ReviewEntity
import com.codealphas.themovie.data.review.local.ReviewSyncState
import com.codealphas.themovie.data.review.remote.ReviewRealtimeDataSource
import com.codealphas.themovie.data.review.remote.ReviewStorageDataSource
import com.codealphas.themovie.data.review.sync.ReviewSyncLock
import com.codealphas.themovie.data.review.sync.ReviewSyncScheduler
import com.codealphas.themovie.domain.auth.AuthRepository
import com.codealphas.themovie.domain.result.Outcome
import com.codealphas.themovie.domain.review.Review
import com.codealphas.themovie.domain.review.ReviewDraft
import com.codealphas.themovie.domain.review.ReviewError
import com.codealphas.themovie.domain.review.ReviewPhoto
import com.codealphas.themovie.domain.review.ReviewRepository
import com.codealphas.themovie.domain.review.ReviewResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

// Realtime Database는 디스크 캐시를 쓰지 않으면 오프라인에서 서버 감상문 읽기를 실패시키지 않고 연결될 때까지 기다리므로,
// 빈 감상문 목록에 로딩 표시만 계속 남지 않도록 서버 읽기 대기 시간 제한
internal val SERVER_READ_TIMEOUT = 15.seconds

internal class ReviewRepositoryImpl
    @Inject
    constructor(
        private val reviewDao: ReviewDao,
        private val realtimeDataSource: ReviewRealtimeDataSource,
        private val storageDataSource: ReviewStorageDataSource,
        private val authRepository: AuthRepository,
        private val clock: ReviewClock,
        private val syncScheduler: ReviewSyncScheduler,
        private val syncLock: ReviewSyncLock,
    ) : ReviewRepository {
        override fun observeAll(): Flow<List<Review>> =
            reviewDao.getAll().map { reviews -> reviews.map(ReviewEntity::toReview) }

        override suspend fun syncFromRemote(): ReviewResult {
            val userId = authRepository.currentUserId() ?: return Outcome.Failure(ReviewError.Unknown)
            return try {
                // 잠금 대기를 시간 제한 밖에 두면 업로더가 감상문을 지우는 동안 목록에 로딩 표시가 그만큼 더 남으므로,
                // 잠금 대기도 서버 읽기 시간 제한에 포함
                val inserted =
                    withTimeoutOrNull(SERVER_READ_TIMEOUT) {
                        // 서버 목록을 읽은 뒤 Room에 넣기 전에 업로더가 같은 감상문을 서버와 Room에서 지우면 지운 감상문이 다시 추가되므로,
                        // 서버 읽기부터 Room 추가까지 업로더의 감상문 삭제와 같은 잠금 적용
                        syncLock.withLock { insertRemoteReviews(userId) }
                    }
                if (inserted == null) {
                    Outcome.Failure(ReviewError.Unknown)
                } else {
                    // 감상문 업로드 작업이 끝난 뒤에도 대기 상태로 남은 감상문은 다른 감상문을 저장하거나 지우기 전까지 다시 예약되지 않으므로,
                    // 앱 업데이트나 규칙 수정으로 원인이 고쳐진 뒤 올라가도록 목록을 열 때 감상문 업로드 다시 예약
                    if (reviewDao.countPending() > 0) syncScheduler.schedule()
                    Outcome.Success(Unit)
                }
            } catch (_: CancellationException) {
                // Firebase Realtime Database SDK가 서버 감상문 읽기를 취소해도 취소 예외로 끝나는데 그대로 올리면 동기화 중 표시가 풀리지 않으므로,
                // 동기화를 요청한 코루틴이 취소되지 않았으면 동기화 실패로 처리
                currentCoroutineContext().ensureActive()
                Outcome.Failure(ReviewError.Unknown)
            } catch (_: Exception) {
                Outcome.Failure(ReviewError.Unknown)
            }
        }

        private suspend fun insertRemoteReviews(userId: String) {
            // 서버에서 받은 감상문은 이미 서버에 있으므로, 다시 올리지 않도록 SYNCED와 localVersion 0으로 추가
            realtimeDataSource.getAll(userId).forEach { review ->
                reviewDao.insert(review.toEntity(syncState = ReviewSyncState.SYNCED, localVersion = 0))
            }
        }

        override suspend fun getById(id: String): Review? = reviewDao.getById(id)?.toReview()

        override suspend fun save(draft: ReviewDraft): ReviewResult {
            val userId = authRepository.currentUserId() ?: return Outcome.Failure(ReviewError.Unknown)
            return try {
                val previous = draft.id?.let { reviewDao.getById(it) }
                // 수정할 감상문이 Room에 없는데 저장하면 같은 id로 새로 추가되어 지운 감상문이 되살아나므로, 저장하지 않고 실패 반환
                if (draft.id != null && previous == null) {
                    Outcome.Failure(ReviewError.Unknown)
                } else {
                    preparePhotoAndSave(draft, previous, userId)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                Outcome.Failure(ReviewError.Unknown)
            }
        }

        private suspend fun preparePhotoAndSave(
            draft: ReviewDraft,
            previous: ReviewEntity?,
            userId: String,
        ): ReviewResult {
            // 키를 만들지 못해 실패할 때 이미 올린 사진이 고아 파일로 남지 않도록, 사진을 Storage에 업로드하기 전에 새 감상문 키 생성 처리
            val id = draft.id ?: realtimeDataSource.newKey(userId)
            val photoFields =
                preparePhotoFields(draft.photo, previous, userId)
                    ?: return Outcome.Failure(ReviewError.PhotoUploadFailed)
            // 저장을 요청한 코루틴이 Room 저장 뒤에 취소되면 감상문 업로드를 예약하거나 업로드 전에 바뀐 사진 파일을
            // 지우지 못한 채 끝나므로, Room 저장을 시작한 뒤에는 취소되지 않고 끝까지 처리
            // 사진을 Storage에 업로드한 직후 프로세스가 종료되면 코드로 막을 수 없고 드물어서, 남는 고아 사진 파일은 별도 정리 없이 허용
            withContext(NonCancellable) { saveAndScheduleUpload(id, draft, previous, photoFields) }
            return Outcome.Success(Unit)
        }

        // 사진을 Storage에 업로드하지 못하면 기존 사진과 Room, 서버를 바꾸지 않도록 null 반환
        private suspend fun preparePhotoFields(
            photo: ReviewPhoto,
            previous: ReviewEntity?,
            userId: String,
        ): PhotoFields? =
            when (photo) {
                ReviewPhoto.Unchanged -> PhotoFields(previous?.image.orEmpty(), previous?.storageFileName.orEmpty())
                ReviewPhoto.Removed -> PhotoFields(imageUrl = "", fileName = "")
                is ReviewPhoto.New -> uploadPhoto(photo.uri, userId)
            }

        private suspend fun uploadPhoto(
            uri: String,
            userId: String,
        ): PhotoFields? {
            // Storage 규칙이 경로의 사용자 폴더로 본인 사진만 쓰고 지우게 막으므로, 사진을 uid 폴더 아래에 저장 처리
            // 갤러리 사진은 JPEG, PNG, WebP 등 형식이 여러 가지라 확장자를 고정하면 실제 형식과 어긋나므로,
            // 형식은 업로드 메타데이터로 지정하고 파일 이름에는 확장자를 붙이지 않도록 처리
            val fileName = "$userId/${clock.nowMillis()}"
            return try {
                PhotoFields(imageUrl = storageDataSource.upload(fileName, uri), fileName = fileName)
            } catch (_: CancellationException) {
                // Firebase Storage SDK가 사진 업로드를 취소해도 취소 예외로 끝나는데 그대로 올리면 저장 중 표시가 풀리지 않으므로,
                // 저장을 요청한 코루틴이 취소되지 않았으면 업로드 실패로 처리
                currentCoroutineContext().ensureActive()
                null
            } catch (_: Exception) {
                null
            }
        }

        private suspend fun saveAndScheduleUpload(
            id: String,
            draft: ReviewDraft,
            previous: ReviewEntity?,
            photoFields: PhotoFields,
        ) {
            val now = clock.nowMillis()
            val cleanup = planPhotoCleanup(draft.photo, previous)
            val entity =
                ReviewEntity(
                    title = draft.title,
                    image = photoFields.imageUrl,
                    content = draft.content,
                    createdAt = previous?.createdAt ?: now,
                    updatedAt = now,
                    rating = draft.rating,
                    storageFileName = photoFields.fileName,
                    storageFileNameToDelete = cleanup.storageFileNameToDelete,
                    syncState = ReviewSyncState.PENDING_UPSERT,
                    // 감상문을 업로드하는 동안 다시 저장하면 업로더가 올린 내용이 Room 내용과 달라지므로,
                    // 업로더가 이를 알아채고 SYNCED로 바꾸지 않도록 저장할 때마다 localVersion 증가
                    localVersion = (previous?.localVersion ?: 0) + 1,
                    id = id,
                )
            // Room 저장 실패를 catch(e: Exception)으로 잡아 사진을 지운 뒤 다시 던지면 detekt TooGenericExceptionCaught에 걸리므로,
            // 예외는 잡지 않고 그대로 올리면서 finally에서 stored가 false일 때만 새 사진 삭제
            var stored = false
            try {
                if (previous == null) reviewDao.insert(entity) else reviewDao.update(entity)
                stored = true
            } finally {
                // Room에 저장되지 않아 방금 올린 사진을 가리키는 곳이 없으므로, 고아 파일이 남지 않게 삭제 처리
                if (!stored && draft.photo is ReviewPhoto.New) deletePhoto(photoFields.fileName)
            }
            syncScheduler.schedule()
            if (cleanup.unreferencedFileName.isNotEmpty()) deletePhoto(cleanup.unreferencedFileName)
        }

        private fun planPhotoCleanup(
            photo: ReviewPhoto,
            previous: ReviewEntity?,
        ): PhotoCleanup {
            val previousFileName = previous?.storageFileName.orEmpty()
            val previousFileNameToDelete = previous?.storageFileNameToDelete.orEmpty()
            // 사진을 그대로 두는데 파일을 지우면 저장된 URL이 죽은 링크가 되므로, 바꾸거나 지운 경우에만 이전 파일 정리
            val isPhotoReplaced = photo !is ReviewPhoto.Unchanged && previousFileName.isNotEmpty()
            return when {
                !isPhotoReplaced ->
                    PhotoCleanup(storageFileNameToDelete = previousFileNameToDelete, unreferencedFileName = "")
                // 서버 감상문은 감상문 업로드가 끝날 때까지 이전 사진을 가리킬 수 있으므로,
                // 이전 사진 파일은 지금 지우지 않고 감상문 업로드가 끝난 뒤 업로더가 지우도록 storageFileNameToDelete에 보관
                previousFileNameToDelete.isEmpty() ->
                    PhotoCleanup(storageFileNameToDelete = previousFileName, unreferencedFileName = "")
                // 사진을 A에서 B로 바꾸고 감상문 업로드가 끝나기 전에 다시 C로 바꾸면 다음 감상문 업로드 뒤 B를 가리키는 곳이 없으므로,
                // Room에 C를 저장한 뒤 Storage의 B 파일은 바로 지우고 A는 감상문 업로드 뒤 지우도록 그대로 보관
                // B를 올리던 감상문 업로드가 이미 진행 중이면 다음 감상문 업로드 전까지 서버 감상문이 지운 B를 가리키지만,
                // 다음 감상문 업로드가 C로 바꾸므로 잠시 사진이 깨져 보이는 것을 허용
                else ->
                    PhotoCleanup(
                        storageFileNameToDelete = previousFileNameToDelete,
                        unreferencedFileName = previousFileName,
                    )
            }
        }

        override suspend fun delete(review: Review): ReviewResult =
            try {
                // 삭제를 요청한 코루틴이 삭제 표시 뒤에 취소되면 감상문 업로드를 예약하지 못해 다음 저장, 삭제, 동기화 성공 전까지 서버에서 지워지지 않으므로,
                // 삭제 표시를 시작한 뒤에는 취소되지 않고 예약까지 처리
                withContext(NonCancellable) {
                    reviewDao.markPendingDelete(review.id)
                    syncScheduler.schedule()
                }
                Outcome.Success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                Outcome.Failure(ReviewError.Unknown)
            }

        override suspend fun deleteAll() {
            reviewDao.deleteAll()
        }

        override suspend fun countPendingChanges(): Int = reviewDao.countPending()

        override fun cancelPendingSync() {
            syncScheduler.cancel()
        }

        private suspend fun deletePhoto(fileName: String) {
            try {
                storageDataSource.delete(fileName)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // 사진 파일을 지우지 못해도 감상문 저장 결과는 바뀌지 않으므로, Storage에 사진 파일이 남는 것을 허용
            }
        }
    }

private data class PhotoFields(
    val imageUrl: String,
    val fileName: String,
)

private data class PhotoCleanup(
    // 감상문 업로드가 끝난 뒤 업로더가 Storage에서 지울 이전 사진 파일 이름
    val storageFileNameToDelete: String,
    // 감상문 업로드가 끝나기 전에 다른 사진으로 바뀌어 Room 저장 뒤 Storage에서 바로 지울 사진 파일 이름
    val unreferencedFileName: String,
)
