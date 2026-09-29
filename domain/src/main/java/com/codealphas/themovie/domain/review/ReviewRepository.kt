package com.codealphas.themovie.domain.review

import kotlinx.coroutines.flow.Flow

interface ReviewRepository {
    fun observeAll(): Flow<List<Review>>

    suspend fun syncFromRemote(): ReviewResult

    suspend fun update(review: Review)

    suspend fun delete(review: Review)

    suspend fun deleteAll()

    suspend fun insertAndReturnId(review: Review): Int
}
