package com.codealphas.themovie.domain.movie

import com.codealphas.themovie.domain.result.DataResult

interface MovieRepository {
    suspend fun getPopularMovies(page: Int): DataResult<MoviePage>

    suspend fun getTopRatedMovies(page: Int): DataResult<MoviePage>

    suspend fun searchMovies(
        query: String,
        page: Int,
    ): DataResult<MoviePage>

    suspend fun getMovieDetail(movieId: Int): MovieDetailResult
}
