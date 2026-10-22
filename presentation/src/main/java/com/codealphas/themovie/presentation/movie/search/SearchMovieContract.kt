package com.codealphas.themovie.presentation.movie.search

data class SearchMovieUiState(
    val query: String = "",
)

sealed interface SearchMovieIntent {
    data class QueryChanged(
        val query: String,
    ) : SearchMovieIntent
}
