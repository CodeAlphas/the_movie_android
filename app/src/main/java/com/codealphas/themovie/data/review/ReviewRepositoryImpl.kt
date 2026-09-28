package com.codealphas.themovie.data.review

import com.codealphas.themovie.data.review.local.ReviewDao
import com.codealphas.themovie.data.review.local.ReviewEntity
import com.codealphas.themovie.domain.review.Review
import com.codealphas.themovie.domain.review.ReviewRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ReviewRepositoryImpl
    @Inject
    constructor(
        private val reviewDao: ReviewDao,
    ) : ReviewRepository {
        override fun observeAll(): Flow<List<Review>> =
            reviewDao.getAll().map { reviews -> reviews.map(ReviewEntity::toReview) }

        override suspend fun insert(review: Review) {
            reviewDao.insert(review.toEntity())
        }

        override suspend fun update(review: Review) {
            reviewDao.update(review.toEntity())
        }

        override suspend fun delete(review: Review) {
            reviewDao.delete(review.toEntity())
        }

        override suspend fun deleteAll() {
            reviewDao.deleteAll()
        }

        override suspend fun insertAndReturnId(review: Review): Int = reviewDao.insertTransaction(review.toEntity())
    }
