package com.codealphas.themovie.presentation.movie

import com.codealphas.themovie.domain.movie.Movie
import com.codealphas.themovie.domain.result.RemoteError

data class SearchMovieUiState(
    val query: String = "",
    val movies: List<Movie>? = null,
    val isLoading: Boolean = false,
    val loadError: RemoteError? = null,
)

sealed interface SearchMovieIntent {
    data class QueryChanged(
        val query: String,
    ) : SearchMovieIntent

    data object RetryClicked : SearchMovieIntent
}

sealed interface SearchMovieEffect {
    data class ShowError(
        val error: RemoteError,
    ) : SearchMovieEffect
}
