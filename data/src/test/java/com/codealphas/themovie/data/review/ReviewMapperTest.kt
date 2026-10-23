package com.codealphas.themovie.data.review

import com.codealphas.themovie.domain.review.Review
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReviewMapperTest {
    @Test
    fun `도메인 Review와 엔티티를 서로 바꾸면 id를 포함한 필드가 유지되어야 한다`() {
        val review =
            Review(
                title = "기생충",
                imageUrl = "https://example.com/poster.jpg",
                content = "잘 봤다",
                createdAt = CREATED_AT,
                updatedAt = UPDATED_AT,
                rating = 9.0,
                storageFileName = "file.png",
                id = "-review-key",
            )

        val entity = review.toEntity()

        assertEquals("기생충", entity.title)
        assertEquals("https://example.com/poster.jpg", entity.image)
        assertEquals("잘 봤다", entity.content)
        assertEquals(CREATED_AT, entity.createdAt)
        assertEquals(UPDATED_AT, entity.updatedAt)
        assertEquals(9.0, entity.rating, 0.0)
        assertEquals("file.png", entity.storageFileName)
        assertEquals("-review-key", entity.id)
        assertEquals(review, entity.toReview())
    }

    @Test
    fun `감상문을 서버 필드로 바꾸면 id를 뺀 모든 필드를 담고 createdAt과 updatedAt은 표시 문자열로 바꾸지 않고 숫자 그대로 담아야 한다`() {
        val fields = REVIEW.toRemoteFields()

        assertEquals(
            mapOf(
                "image" to "https://example.com/photo.png",
                "title" to "기생충",
                "content" to "잘 봤다",
                "createdAt" to CREATED_AT,
                "updatedAt" to UPDATED_AT,
                "rating" to 9.0,
                "storageFileName" to "photo.png",
            ),
            fields,
        )
    }

    @Test
    fun `서버 감상문의 필드가 모두 정상 형식이면 필드 값을 그대로 옮기고 서버 키를 id로 담아야 한다`() {
        val review = remoteReviewOrNull(id = REVIEW.id, fields = serverFields())

        assertEquals(REVIEW, review)
    }

    @Test
    fun `Realtime Database가 정수 평점을 Long으로 주면 Double 평점으로 읽어야 한다`() {
        val review = remoteReviewOrNull(id = REVIEW.id, fields = serverFields() + ("rating" to 9L))

        assertEquals(9.0, review?.rating)
    }

    @Test
    fun `createdAt과 updatedAt이 없고 예전 형식의 문자열 time만 있으면 그 감상문을 건너뛰어야 한다`() {
        val fields = serverFields() - "createdAt" - "updatedAt" + ("time" to "2024/01/02 03:04")

        assertNull(remoteReviewOrNull(id = REVIEW.id, fields = fields))
    }

    @Test
    fun `updatedAt이 숫자가 아닌 문자열이면 그 감상문을 건너뛰어야 한다`() {
        val fields = serverFields() + ("updatedAt" to "1650000000000")

        assertNull(remoteReviewOrNull(id = REVIEW.id, fields = fields))
    }

    @Test
    fun `서버 감상문에 필드가 하나도 없으면 그 감상문을 건너뛰어야 한다`() {
        assertNull(remoteReviewOrNull(id = REVIEW.id, fields = null))
    }
}

private const val CREATED_AT = 1_600_000_000_000L
private const val UPDATED_AT = 1_650_000_000_000L

private val REVIEW =
    Review(
        title = "기생충",
        imageUrl = "https://example.com/photo.png",
        content = "잘 봤다",
        createdAt = CREATED_AT,
        updatedAt = UPDATED_AT,
        rating = 9.0,
        storageFileName = "photo.png",
        id = "-review",
    )

private fun serverFields(): Map<String, Any> =
    mapOf(
        "image" to "https://example.com/photo.png",
        "title" to "기생충",
        "content" to "잘 봤다",
        "createdAt" to CREATED_AT,
        "updatedAt" to UPDATED_AT,
        "rating" to 9.0,
        "storageFileName" to "photo.png",
    )
