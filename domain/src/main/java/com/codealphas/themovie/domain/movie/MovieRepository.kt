package com.codealphas.themovie.domain.movie

import androidx.paging.PagingData
import com.codealphas.themovie.domain.result.DataResult
import kotlinx.coroutines.flow.Flow

interface MovieRepository {
    fun getPopularMovies(): Flow<PagingData<Movie>>

    fun getTopRatedMovies(): Flow<PagingData<Movie>>

    suspend fun searchMovies(
        query: String,
        page: Int,
    ): DataResult<MoviePage>

    suspend fun getMovieDetail(movieId: Int): MovieDetailResult
}
