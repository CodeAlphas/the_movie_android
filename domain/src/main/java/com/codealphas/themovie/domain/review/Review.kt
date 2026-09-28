package com.codealphas.themovie.domain.review

data class Review(
    val title: String,
    val image: String,
    val content: String,
    val time: String,
    val rating: Double,
    val storageFileName: String,
    val id: Int = 0,
)
