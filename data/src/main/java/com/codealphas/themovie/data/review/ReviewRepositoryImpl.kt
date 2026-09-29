package com.codealphas.themovie.data.review

import com.codealphas.themovie.data.review.local.ReviewDao
import com.codealphas.themovie.data.review.local.ReviewEntity
import com.codealphas.themovie.data.review.remote.ReviewRealtimeDataSource
import com.codealphas.themovie.data.review.remote.ReviewStorageDataSource
import com.codealphas.themovie.domain.auth.AuthRepository
import com.codealphas.themovie.domain.review.Review
import com.codealphas.themovie.domain.review.ReviewError
import com.codealphas.themovie.domain.review.ReviewRepository
import com.codealphas.themovie.domain.review.ReviewResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class ReviewRepositoryImpl
    @Inject
    constructor(
        private val reviewDao: ReviewDao,
        private val realtimeDataSource: ReviewRealtimeDataSource,
        private val storageDataSource: ReviewStorageDataSource,
        private val authRepository: AuthRepository,
    ) : ReviewRepository {
        override fun observeAll(): Flow<List<Review>> =
            reviewDao.getAll().map { reviews -> reviews.map(ReviewEntity::toReview) }

        // DAO insert가 IGNORE라 Room에 이미 있는 id는 서버 값으로 덮어쓰지 않음
        override suspend fun syncFromRemote(): ReviewResult {
            val userId = authRepository.currentUserId() ?: return ReviewResult.Failure(ReviewError.Unknown)
            return try {
                realtimeDataSource.getAll(userId).forEach { reviewDao.insert(it.toEntity()) }
                ReviewResult.Success
            } catch (e: CancellationException) {
                // 취소를 Failure로 바꾸면 ViewModel이 사라진 뒤에도 실패 안내가 뜨므로, 호출한 코루틴이 멈추도록 취소 예외를 다시 던짐
                throw e
            } catch (_: Exception) {
                ReviewResult.Failure(ReviewError.Unknown)
            }
        }

        override suspend fun update(review: Review) {
            reviewDao.update(review.toEntity())
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

        override suspend fun insertAndReturnId(review: Review): Int = reviewDao.insertTransaction(review.toEntity())

        private suspend fun deleteStorageFile(fileName: String) {
            try {
                storageDataSource.delete(fileName)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Room과 서버 삭제는 끝났고 남은 사진 파일만 정리하지 못한 것이므로, 감상문 삭제는 유지
            }
        }
    }
