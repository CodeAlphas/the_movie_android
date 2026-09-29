package com.codealphas.themovie.data.review.remote

import com.codealphas.themovie.domain.review.Review

internal interface ReviewRealtimeDataSource {
    suspend fun getAll(userId: String): List<Review>

    fun delete(
        userId: String,
        reviewId: Int,
    )
}
