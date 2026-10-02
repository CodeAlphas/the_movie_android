package com.codealphas.themovie.data.review

import com.codealphas.themovie.data.review.local.ReviewDao
import com.codealphas.themovie.data.review.local.ReviewEntity
import com.codealphas.themovie.data.review.remote.ReviewRealtimeDataSource
import com.codealphas.themovie.data.review.remote.ReviewStorageDataSource
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

private const val USER_ID_PREFIX_LENGTH = 10

internal class ReviewRepositoryImpl
    @Inject
    constructor(
        private val reviewDao: ReviewDao,
        private val realtimeDataSource: ReviewRealtimeDataSource,
        private val storageDataSource: ReviewStorageDataSource,
        private val authRepository: AuthRepository,
        private val clock: ReviewClock,
    ) : ReviewRepository {
        override fun observeAll(): Flow<List<Review>> =
            reviewDao.getAll().map { reviews -> reviews.map(ReviewEntity::toReview) }

        // DAO insert가 IGNORE라 Room에 이미 있는 id는 서버 값으로 덮어쓰지 않음
        override suspend fun syncFromRemote(): ReviewResult {
            val userId = authRepository.currentUserId() ?: return Outcome.Failure(ReviewError.Unknown)
            return try {
                realtimeDataSource.getAll(userId).forEach { reviewDao.insert(it.toEntity()) }
                Outcome.Success(Unit)
            } catch (e: CancellationException) {
                // 취소를 Failure로 바꾸면 ViewModel이 사라진 뒤에도 실패 안내가 뜨므로, 호출한 코루틴이 멈추도록 취소 예외를 다시 던짐
                throw e
            } catch (_: Exception) {
                Outcome.Failure(ReviewError.Unknown)
            }
        }

        override suspend fun getById(id: Int): Review? = reviewDao.getById(id)?.toReview()

        override suspend fun save(draft: ReviewDraft): ReviewResult {
            val userId = authRepository.currentUserId() ?: return Outcome.Failure(ReviewError.Unknown)
            return try {
                val previous = draft.id?.let { reviewDao.getById(it) }
                // 수정할 감상문이 Room에 없는데 저장하면 Room은 바뀌지 않고 서버에만 쓰이므로, 저장하지 않고 실패 반환
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
            val photo =
                resolvePhoto(draft.photo, previous, userId)
                    ?: return Outcome.Failure(ReviewError.PhotoUploadFailed)
            // 화면을 닫아 취소되면 Room에는 저장됐는데 서버에는 쓰이지 않은 상태로 남으므로,
            // 업로드가 끝난 뒤의 저장 단계는 취소되지 않고 끝까지 처리.
            // 업로드 직후 프로세스가 종료되면 코드로 막을 수 없고 드물어서, 남는 고아 사진 파일은 별도 정리 없이 허용
            withContext(NonCancellable) { persist(draft, previous, photo, userId) }
            return Outcome.Success(Unit)
        }

        // 업로드가 실패하면 null. 이때 기존 사진과 Room, 서버는 그대로
        private suspend fun resolvePhoto(
            photo: ReviewPhoto,
            previous: ReviewEntity?,
            userId: String,
        ): PhotoFields? =
            when (photo) {
                ReviewPhoto.Unchanged -> PhotoFields(previous?.image.orEmpty(), previous?.storageFileName.orEmpty())
                ReviewPhoto.Removed -> PhotoFields(image = "", fileName = "")
                is ReviewPhoto.New -> upload(photo.uri, userId)
            }

        private suspend fun upload(
            uri: String,
            userId: String,
        ): PhotoFields? {
            val fileName = userId.take(USER_ID_PREFIX_LENGTH) + "${clock.nowMillis()}.png"
            return try {
                PhotoFields(image = storageDataSource.upload(fileName, uri), fileName = fileName)
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
            draft: ReviewDraft,
            previous: ReviewEntity?,
            photo: PhotoFields,
            userId: String,
        ) {
            val existingId = draft.id
            val entity =
                ReviewEntity(
                    title = draft.title,
                    image = photo.image,
                    content = draft.content,
                    time = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.KOREA).format(Date(clock.nowMillis())),
                    rating = draft.rating,
                    storageFileName = photo.fileName,
                    id = existingId ?: 0,
                )
            // 예외를 잡아 다시 던지면 detekt가 막으므로, 저장 성공 여부를 플래그에 남겨 finally에서 실패일 때만 정리 처리
            var stored = false
            val id =
                try {
                    val savedId =
                        if (existingId == null) {
                            reviewDao.insert(entity).toInt()
                        } else {
                            reviewDao.update(entity)
                            existingId
                        }
                    stored = true
                    savedId
                } finally {
                    // Room에 저장되지 않아 방금 올린 사진을 가리키는 곳이 없으므로, 고아 파일이 남지 않게 삭제 처리
                    if (!stored && draft.photo is ReviewPhoto.New) deleteStorageFile(photo.fileName)
                }
            realtimeDataSource.save(userId, entity.toReview().copy(id = id))

            // 사진을 그대로 두는데 파일을 지우면 저장된 URL이 죽은 링크가 되므로, 바꾸거나 지운 경우에만 기존 파일 삭제
            val previousFileName = previous?.storageFileName.orEmpty()
            if (draft.photo !is ReviewPhoto.Unchanged && previousFileName.isNotEmpty()) {
                deleteStorageFile(previousFileName)
            }
        }

        override suspend fun delete(review: Review) {
            reviewDao.delete(review.toEntity())
            val userId = authRepository.currentUserId() ?: return
            realtimeDataSource.delete(userId, review.id)
            if (review.storageFileName.isNotEmpty()) {
                deleteStorageFile(review.storageFileName)
            }
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
    val image: String,
    val fileName: String,
)
