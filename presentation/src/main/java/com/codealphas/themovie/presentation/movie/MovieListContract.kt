package com.codealphas.themovie.presentation.movie

import com.codealphas.themovie.domain.movie.Movie
import com.codealphas.themovie.domain.result.RemoteError

data class MovieListUiState(
    val movies: List<Movie>? = null,
    val isLoading: Boolean = false,
    val loadError: RemoteError? = null,
)

sealed interface MovieListIntent {
    data object PageShown : MovieListIntent

    data object RetryClicked : MovieListIntent
}

sealed interface MovieListEffect {
    data class ShowError(
        val error: RemoteError,
    ) : MovieListEffect
}
