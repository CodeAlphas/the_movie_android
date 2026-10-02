package com.codealphas.themovie.presentation.movie

import com.codealphas.themovie.domain.movie.MovieDetail
import com.codealphas.themovie.domain.result.RemoteError

data class MovieDetailUiState(
    val detail: MovieDetail? = null,
    val isLoading: Boolean = true,
    val loadError: RemoteError? = null,
)

sealed interface MovieDetailIntent {
    data object RetryClicked : MovieDetailIntent
}

sealed interface MovieDetailEffect {
    data class ShowError(
        val error: RemoteError,
    ) : MovieDetailEffect
}
