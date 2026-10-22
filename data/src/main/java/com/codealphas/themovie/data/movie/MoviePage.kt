package com.codealphas.themovie.data.movie

import com.codealphas.themovie.domain.movie.Movie

internal data class MoviePage(
    val movies: List<Movie>,
    val page: Int,
    val totalPages: Int,
)
