package com.codealphas.themovie.data.review

import com.codealphas.themovie.data.review.local.ReviewEntity
import com.codealphas.themovie.domain.review.Review

internal fun ReviewEntity.toReview(): Review =
    Review(
        title = title,
        imageUrl = image,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt,
        rating = rating,
        storageFileName = storageFileName,
        id = id,
    )

internal fun Review.toEntity(): ReviewEntity =
    ReviewEntity(
        title = title,
        image = imageUrl,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt,
        rating = rating,
        storageFileName = storageFileName,
        id = id,
    )

internal fun Review.toRemoteFields(): Map<String, Any> =
    mapOf(
        "image" to imageUrl,
        "title" to title,
        "content" to content,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "rating" to rating,
        "storageFileName" to storageFileName,
    )

// 필드가 빠진 항목 하나가 예외를 던지면 목록 전체 동기화가 중단되므로, 읽지 못한 항목만 제외
internal fun remoteReviewOrNull(
    id: String,
    fields: Map<*, *>?,
): Review? {
    val rating = fields?.get("rating")?.toString()?.toDoubleOrNull()
    // Realtime Database SDK는 정수를 Int가 아닌 Long으로 돌려주므로, 시각을 Long으로 읽고 예전 형식의 문자열 시각 같은 다른 타입은 null로 처리
    val createdAt = fields?.get("createdAt") as? Long
    val updatedAt = fields?.get("updatedAt") as? Long
    if (rating == null || createdAt == null || updatedAt == null) return null
    return Review(
        title = fields["title"].toString(),
        imageUrl = fields["image"].toString(),
        content = fields["content"].toString(),
        createdAt = createdAt,
        updatedAt = updatedAt,
        rating = rating,
        storageFileName = fields["storageFileName"].toString(),
        id = id,
    )
}
