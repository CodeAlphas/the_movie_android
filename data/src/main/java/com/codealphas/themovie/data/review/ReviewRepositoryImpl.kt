package com.codealphas.themovie.data.review

import com.codealphas.themovie.data.review.local.ReviewDao
import com.codealphas.themovie.data.review.local.ReviewEntity
import com.codealphas.themovie.data.review.local.ReviewSyncState
import com.codealphas.themovie.data.review.remote.ReviewRealtimeDataSource
import com.codealphas.themovie.data.review.remote.ReviewStorageDataSource
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
import javax.inject.Inject

internal class ReviewRepositoryImpl
    @Inject
    constructor(
        private val reviewDao: ReviewDao,
        private val realtimeDataSource: ReviewRealtimeDataSource,
        private val storageDataSource: ReviewStorageDataSource,
        private val authRepository: AuthRepository,
        private val clock: ReviewClock,
        private val syncScheduler: ReviewSyncScheduler,
    ) : ReviewRepository {
        override fun observeAll(): Flow<List<Review>> =
            reviewDao.getAll().map { reviews -> reviews.map(ReviewEntity::toReview) }

        override suspend fun syncFromRemote(): ReviewResult {
            val userId = authRepository.currentUserId() ?: return Outcome.Failure(ReviewError.Unknown)
            return try {
                // 서버에서 받은 감상문은 이미 서버에 있으므로, 다시 올리지 않도록 SYNCED와 localVersion 0으로 추가
                realtimeDataSource.getAll(userId).forEach { review ->
                    reviewDao.insert(review.toEntity(syncState = ReviewSyncState.SYNCED, localVersion = 0))
                }
                // 업로드 작업이 끝난 뒤에도 대기 상태로 남은 감상문은 새로 저장하기 전까지 다시 예약되지 않으므로,
                // 앱 업데이트나 규칙 수정으로 원인이 고쳐진 뒤 올라가도록 목록을 열 때 업로드 다시 예약
                if (reviewDao.getPending().isNotEmpty()) syncScheduler.schedule()
                Outcome.Success(Unit)
            } catch (_: CancellationException) {
                // Firebase Realtime Database SDK가 서버 감상문 읽기를 취소해도 취소 예외로 끝나는데 그대로 올리면 동기화 중 표시가 풀리지 않으므로,
                // 동기화를 요청한 코루틴이 취소되지 않았으면 동기화 실패로 처리
                currentCoroutineContext().ensureActive()
                Outcome.Failure(ReviewError.Unknown)
            } catch (_: Exception) {
                Outcome.Failure(ReviewError.Unknown)
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
                    store(draft, previous, userId)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                Outcome.Failure(ReviewError.Unknown)
            }
        }

        private suspend fun store(
            draft: ReviewDraft,
            previous: ReviewEntity?,
            userId: String,
        ): ReviewResult {
            // 키를 만들지 못해 실패할 때 이미 올린 사진이 고아 파일로 남지 않도록, 사진 업로드 전에 새 감상문 키 생성 처리
            val id = draft.id ?: realtimeDataSource.newKey(userId)
            val photo =
                resolvePhoto(draft.photo, previous, userId)
                    ?: return Outcome.Failure(ReviewError.PhotoUploadFailed)
            // 저장을 요청한 코루틴이 Room 저장 뒤에 취소되면 업로드를 예약하거나 이전 사진을 지우지 못한 채 끝나므로,
            // Room 저장을 시작한 뒤에는 취소되지 않고 끝까지 처리
            // 업로드 직후 프로세스가 종료되면 코드로 막을 수 없고 드물어서, 남는 고아 사진 파일은 별도 정리 없이 허용
            withContext(NonCancellable) { persist(id, draft, previous, photo) }
            return Outcome.Success(Unit)
        }

        // 업로드가 실패하면 기존 사진과 Room, 서버를 바꾸지 않도록 null 반환
        private suspend fun resolvePhoto(
            photo: ReviewPhoto,
            previous: ReviewEntity?,
            userId: String,
        ): PhotoFields? =
            when (photo) {
                ReviewPhoto.Unchanged -> PhotoFields(previous?.image.orEmpty(), previous?.storageFileName.orEmpty())
                ReviewPhoto.Removed -> PhotoFields(imageUrl = "", fileName = "")
                is ReviewPhoto.New -> upload(photo.uri, userId)
            }

        private suspend fun upload(
            uri: String,
            userId: String,
        ): PhotoFields? {
            // Storage 규칙이 경로의 사용자 폴더로 본인 사진만 쓰고 지우게 막으므로, 사진을 uid 폴더 아래에 저장 처리
            val fileName = "$userId/${clock.nowMillis()}.png"
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

        private suspend fun persist(
            id: String,
            draft: ReviewDraft,
            previous: ReviewEntity?,
            photo: PhotoFields,
        ) {
            val now = clock.nowMillis()
            val entity =
                ReviewEntity(
                    title = draft.title,
                    image = photo.imageUrl,
                    content = draft.content,
                    createdAt = previous?.createdAt ?: now,
                    updatedAt = now,
                    rating = draft.rating,
                    storageFileName = photo.fileName,
                    syncState = ReviewSyncState.PENDING_UPSERT,
                    // 업로드하는 동안 다시 저장하면 업로더가 올린 내용이 Room 내용과 달라지므로,
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
                if (!stored && draft.photo is ReviewPhoto.New) deleteStorageFile(photo.fileName)
            }
            syncScheduler.schedule()

            // 사진을 그대로 두는데 파일을 지우면 저장된 URL이 죽은 링크가 되므로, 바꾸거나 지운 경우에만 기존 파일 삭제
            val previousFileName = previous?.storageFileName.orEmpty()
            if (draft.photo !is ReviewPhoto.Unchanged && previousFileName.isNotEmpty()) {
                deleteStorageFile(previousFileName)
            }
        }

        override suspend fun delete(review: Review): ReviewResult {
            try {
                // Room의 @Delete는 넘긴 엔티티에서 기본 키(id)만 읽어 지울 행을 찾으므로,
                // toEntity()가 요구하는 syncState와 localVersion에는 어떤 값을 넘겨도 같아 SYNCED와 0을 전달
                reviewDao.delete(review.toEntity(syncState = ReviewSyncState.SYNCED, localVersion = 0))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Room에서 지우지 못한 감상문은 목록에 남아 서버 백업과 사진을 지우면 사진이 깨지므로, 서버와 Storage는 그대로 두고 실패 반환
                return Outcome.Failure(ReviewError.Unknown)
            }
            val userId = authRepository.currentUserId()
            if (userId != null) {
                val remoteDeleted =
                    try {
                        realtimeDataSource.delete(userId, review.id)
                        true
                    } catch (_: Exception) {
                        // 서버 삭제 요청이 바로 실패하면 서버에 남은 감상문이 다음 동기화 때 되살아나므로,
                        // 되살아난 감상문의 사진이 깨지지 않도록 사진 파일은 지우지 않고 Room 삭제 결과대로 성공 처리
                        false
                    }
                if (remoteDeleted && review.storageFileName.isNotEmpty()) {
                    deleteStorageFile(review.storageFileName)
                }
            }
            return Outcome.Success(Unit)
        }

        override suspend fun deleteAll() {
            reviewDao.deleteAll()
        }

        private suspend fun deleteStorageFile(fileName: String) {
            try {
                storageDataSource.delete(fileName)
            } catch (_: CancellationException) {
                // Firebase Storage SDK가 사진 삭제를 취소해도 취소 예외로 끝나는데 그대로 올리면 Room에 반영된 저장이나 삭제가 완료로 처리되지 않으므로,
                // 요청한 코루틴이 취소되지 않았으면 Storage에 사진 파일이 남는 것을 허용
                currentCoroutineContext().ensureActive()
            } catch (_: Exception) {
                // 사진 파일을 지우지 못해도 감상문 저장이나 삭제 결과는 바뀌지 않으므로, Storage에 사진 파일이 남는 것을 허용
            }
        }
    }

private data class PhotoFields(
    val imageUrl: String,
    val fileName: String,
)
