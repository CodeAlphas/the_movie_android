package com.codealphas.themovie.domain.movie

data class MovieDetail(
    val id: Int,
    val title: String,
    val posterUrl: String?,
    val releaseDate: String,
    val overview: String,
    val voteAverage: Double,
    val cast: List<Cast>,
    val videos: List<Video>,
)
