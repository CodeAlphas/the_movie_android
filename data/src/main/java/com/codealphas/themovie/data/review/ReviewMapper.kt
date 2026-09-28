package com.codealphas.themovie.data.review

import com.codealphas.themovie.data.review.local.ReviewEntity
import com.codealphas.themovie.domain.review.Review

fun ReviewEntity.toReview(): Review =
    Review(
        title = title,
        image = image,
        content = content,
        time = time,
        rating = rating,
        storageFileName = storageFileName,
        id = id,
    )

fun Review.toEntity(): ReviewEntity =
    ReviewEntity(
        title = title,
        image = image,
        content = content,
        time = time,
        rating = rating,
        storageFileName = storageFileName,
        id = id,
    )
