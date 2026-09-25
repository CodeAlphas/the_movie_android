package com.codealphas.themovie.repository

import androidx.lifecycle.LiveData
import com.codealphas.themovie.database.ReviewDao
import com.codealphas.themovie.models.Review

class ReviewRepository(
    private val reviewDao: ReviewDao,
) {
    val allReview: LiveData<List<Review>> = reviewDao.getAll()

    suspend fun insert(review: Review) {
        reviewDao.insert(review)
    }

    suspend fun delete(review: Review) {
        reviewDao.delete(review)
    }

    suspend fun update(review: Review) {
        reviewDao.update(review)
    }

    suspend fun insertTransaction(review: Review): Int = reviewDao.insertTransaction(review)

    fun deleteAll() {
        reviewDao.deleteAll()
    }
}
