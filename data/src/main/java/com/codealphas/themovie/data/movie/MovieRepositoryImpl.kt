package com.codealphas.themovie.data.movie

import com.codealphas.themovie.data.movie.remote.CreditItem
import com.codealphas.themovie.data.movie.remote.MovieItem
import com.codealphas.themovie.data.movie.remote.TmdbApiService
import com.codealphas.themovie.data.movie.remote.VideoItem
import com.codealphas.themovie.data.remote.safeApiCall
import com.codealphas.themovie.domain.movie.Cast
import com.codealphas.themovie.domain.movie.Movie
import com.codealphas.themovie.domain.movie.MovieRepository
import com.codealphas.themovie.domain.movie.Video
import com.codealphas.themovie.domain.result.DataResult
import com.codealphas.themovie.domain.result.map
import javax.inject.Inject

internal class MovieRepositoryImpl
    @Inject
    constructor(
        private val service: TmdbApiService,
    ) : MovieRepository {
        override suspend fun getPopularMovies(): DataResult<List<Movie>> =
            safeApiCall { service.getPopularMovieList() }.map { it.results.map(MovieItem::toMovie) }

        override suspend fun getTopRatedMovies(): DataResult<List<Movie>> =
            safeApiCall { service.getTopRatedMovieList() }.map { it.results.map(MovieItem::toMovie) }

        override suspend fun searchMovies(query: String): DataResult<List<Movie>> =
            safeApiCall { service.getSearchedMovieList(query = query) }.map { it.results.map(MovieItem::toMovie) }

        override suspend fun getVideos(movieId: Int): DataResult<List<Video>> =
            safeApiCall { service.getVideosList(movieId = movieId) }.map { it.results.map(VideoItem::toVideo) }

        override suspend fun getCast(movieId: Int): DataResult<List<Cast>> =
            safeApiCall { service.getCreditsList(movieId = movieId) }.map { it.cast.map(CreditItem::toCast) }
    }
