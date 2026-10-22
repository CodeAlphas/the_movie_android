package com.codealphas.themovie.domain.movie

data class MoviePage(
    val movies: List<Movie>,
    val page: Int,
    val totalPages: Int,
)
