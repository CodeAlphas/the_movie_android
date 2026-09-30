package com.codealphas.themovie.domain.auth

import com.codealphas.themovie.domain.review.Review
import com.codealphas.themovie.domain.review.ReviewDraft
import com.codealphas.themovie.domain.review.ReviewRepository
import com.codealphas.themovie.domain.review.ReviewResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LogoutUseCaseTest {
    @Test
    fun `로그아웃하면 signOut 다음에 deleteAll을 호출해야 한다`() =
        runTest {
            val calls = mutableListOf<String>()
            val useCase =
                LogoutUseCase(
                    authRepository = FakeAuthRepository(calls),
                    reviewRepository = FakeReviewRepository(calls),
                )

            useCase()

            assertEquals(listOf("signOut", "deleteAll"), calls)
        }
}

private class FakeAuthRepository(
    private val calls: MutableList<String>,
) : AuthRepository {
    override suspend fun signIn(
        email: String,
        password: String,
    ): AuthResult = error("사용하지 않음")

    override suspend fun signUp(
        email: String,
        password: String,
    ): AuthResult = error("사용하지 않음")

    override fun currentUserId(): String? = error("사용하지 않음")

    override fun signOut() {
        calls += "signOut"
    }
}

private class FakeReviewRepository(
    private val calls: MutableList<String>,
) : ReviewRepository {
    override fun observeAll(): Flow<List<Review>> = error("사용하지 않음")

    override suspend fun syncFromRemote(): ReviewResult = error("사용하지 않음")

    override suspend fun getById(id: Int): Review? = error("사용하지 않음")

    override suspend fun save(draft: ReviewDraft): ReviewResult = error("사용하지 않음")

    override suspend fun delete(review: Review) = error("사용하지 않음")

    override suspend fun deleteAll() {
        calls += "deleteAll"
    }
}
