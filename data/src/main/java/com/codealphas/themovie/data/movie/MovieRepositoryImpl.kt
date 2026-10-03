package com.codealphas.themovie.data.movie

import com.codealphas.themovie.data.movie.remote.MovieDto
import com.codealphas.themovie.data.movie.remote.TmdbApiService
import com.codealphas.themovie.data.remote.safeApiCall
import com.codealphas.themovie.domain.movie.Movie
import com.codealphas.themovie.domain.movie.MovieDetailResult
import com.codealphas.themovie.domain.movie.MovieRepository
import com.codealphas.themovie.domain.result.DataResult
import com.codealphas.themovie.domain.result.map
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

internal class MovieRepositoryImpl
    @Inject
    constructor(
        private val service: TmdbApiService,
    ) : MovieRepository {
        override suspend fun getPopularMovies(): DataResult<List<Movie>> =
            safeApiCall { service.getPopularMovieList() }.map { it.results.map(MovieDto::toMovie) }

        override suspend fun getTopRatedMovies(): DataResult<List<Movie>> =
            safeApiCall { service.getTopRatedMovieList() }.map { it.results.map(MovieDto::toMovie) }

        override suspend fun searchMovies(query: String): DataResult<List<Movie>> =
            safeApiCall { service.getSearchedMovieList(query = query) }.map { it.results.map(MovieDto::toMovie) }

        override suspend fun getMovieDetail(movieId: Int): MovieDetailResult =
            coroutineScope {
                // 세 요청을 순서대로 기다리면 로딩 시간이 세 요청 시간의 합이 되므로, 동시에 보내 결과를 합치도록 async 적용
                val detail = async { safeApiCall { service.getMovieDetail(movieId = movieId) } }
                val cast = async { safeApiCall { service.getCreditsList(movieId = movieId) } }
                val videos = async { safeApiCall { service.getVideosList(movieId = movieId) } }
                toMovieDetailResult(detail.await(), cast.await(), videos.await())
            }
    }
