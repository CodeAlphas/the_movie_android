package com.codealphas.themovie.data.review

import com.codealphas.themovie.data.review.local.ReviewEntity
import com.codealphas.themovie.data.review.local.ReviewSyncState
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

internal fun Review.toEntity(
    syncState: ReviewSyncState,
    localVersion: Int,
): ReviewEntity =
    ReviewEntity(
        title = title,
        image = imageUrl,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt,
        rating = rating,
        storageFileName = storageFileName,
        // 서버에서 받은 감상문에는 Storage에서 지울 이전 사진이 없으므로, 빈 문자열 적용
        storageFileNameToDelete = "",
        syncState = syncState,
        localVersion = localVersion,
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
internal fun toReviewOrNull(
    id: String,
    fields: Map<*, *>?,
): Review? {
    val rating = fields?.get("rating")?.toString()?.toDoubleOrNull()
    // Realtime Database SDK는 정수를 Int가 아닌 Long으로 돌려주므로, 시각을 Long으로 읽고 예전 형식의 문자열 시각 같은 다른 타입은 null로 처리
    val createdAt = fields?.get("createdAt") as? Long
    val updatedAt = fields?.get("updatedAt") as? Long
    if (rating == null || createdAt == null || updatedAt == null) return null
    return Review(
        title = fields.stringOrEmpty("title"),
        imageUrl = fields.stringOrEmpty("image"),
        content = fields.stringOrEmpty("content"),
        createdAt = createdAt,
        updatedAt = updatedAt,
        rating = rating,
        storageFileName = fields.stringOrEmpty("storageFileName"),
        id = id,
    )
}

// 문자열 필드 하나가 빠졌다고 감상문을 건너뛰면 사용자가 쓴 감상문이 목록에서 사라지므로,
// 빠지거나 문자열이 아닌 필드는 빈 문자열 적용
// 사진이 없는 감상문도 사진 주소와 파일 이름이 빈 문자열이므로, 사진 필드가 빠진 감상문은 사진이 없는 감상문으로 처리
private fun Map<*, *>.stringOrEmpty(key: String): String = get(key) as? String ?: ""
