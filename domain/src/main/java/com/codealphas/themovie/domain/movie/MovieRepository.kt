package com.codealphas.themovie.domain.movie

import com.codealphas.themovie.domain.result.DataResult

interface MovieRepository {
    suspend fun getPopularMovies(): DataResult<List<Movie>>

    suspend fun getTopRatedMovies(): DataResult<List<Movie>>

    suspend fun searchMovies(query: String): DataResult<List<Movie>>

    suspend fun getMovieDetail(movieId: Int): MovieDetailResult
}
