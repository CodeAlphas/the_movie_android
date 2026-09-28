package com.codealphas.themovie.data.review

import com.codealphas.themovie.domain.review.Review
import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewMapperTest {
    @Test
    fun `도메인 Review와 엔티티를 서로 바꾸면 id를 포함한 필드가 유지되어야 한다`() {
        val review =
            Review(
                title = "기생충",
                image = "https://example.com/poster.jpg",
                content = "잘 봤다",
                time = "2024/01/02 03:04",
                rating = 9.0,
                storageFileName = "file.png",
                id = 7,
            )

        val entity = review.toEntity()

        assertEquals("기생충", entity.title)
        assertEquals("https://example.com/poster.jpg", entity.image)
        assertEquals("잘 봤다", entity.content)
        assertEquals("2024/01/02 03:04", entity.time)
        assertEquals(9.0, entity.rating, 0.0)
        assertEquals("file.png", entity.storageFileName)
        assertEquals(7, entity.id)
        assertEquals(review, entity.toReview())
    }
}
