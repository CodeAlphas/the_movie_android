package com.codealphas.themovie.domain.review

data class Review(
    val title: String,
    val imageUrl: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val rating: Double,
    val storageFileName: String,
    val id: String,
)
