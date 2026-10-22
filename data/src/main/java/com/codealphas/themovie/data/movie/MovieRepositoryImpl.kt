package com.codealphas.themovie.data.movie

import androidx.paging.Pager
import androidx.paging.PagingData
import com.codealphas.themovie.data.movie.remote.MoviesDto
import com.codealphas.themovie.data.movie.remote.TmdbApiService
import com.codealphas.themovie.data.network.safeApiCall
import com.codealphas.themovie.domain.movie.Movie
import com.codealphas.themovie.domain.movie.MovieDetailResult
import com.codealphas.themovie.domain.movie.MoviePage
import com.codealphas.themovie.domain.movie.MovieRepository
import com.codealphas.themovie.domain.result.DataResult
import com.codealphas.themovie.domain.result.map
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class MovieRepositoryImpl
    @Inject
    constructor(
        private val service: TmdbApiService,
    ) : MovieRepository {
        override fun getPopularMovies(): Flow<PagingData<Movie>> = pagedMovies { service.getPopularMovies(page = it) }

        override fun getTopRatedMovies(): Flow<PagingData<Movie>> = pagedMovies { service.getTopRatedMovies(page = it) }

        override suspend fun searchMovies(
            query: String,
            page: Int,
        ): DataResult<MoviePage> =
            safeApiCall { service.searchMovies(query = query, page = page) }.map(MoviesDto::toMoviePage)

        private fun pagedMovies(request: suspend (page: Int) -> MoviesDto): Flow<PagingData<Movie>> =
            Pager(MoviePagingConfig) {
                MoviePagingSource { page -> safeApiCall { request(page) }.map(MoviesDto::toMoviePage) }
            }.flow

        override suspend fun getMovieDetail(movieId: Int): MovieDetailResult =
            coroutineScope {
                // 세 요청을 순서대로 기다리면 로딩 시간이 세 요청 시간의 합이 되므로, 동시에 보내 결과를 합치도록 async 적용
                val detail = async { safeApiCall { service.getMovieDetail(movieId = movieId) } }
                val cast = async { safeApiCall { service.getCredits(movieId = movieId) } }
                val videos = async { safeApiCall { service.getVideos(movieId = movieId) } }
                toMovieDetailResult(detail.await(), cast.await(), videos.await())
            }
    }
