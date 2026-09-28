package com.codealphas.themovie.domain.movie

data class Movie(
    val id: Int,
    val title: String,
    val posterUrl: String?,
    val releaseDate: String,
    val overview: String,
    val voteAverage: Double,
)
