package com.codealphas.themovie.data.movie.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

internal interface TmdbApiService {
    @GET("movie/popular")
    suspend fun getPopularMovieList(
        @Query("language") language: String = "ko",
        @Query("page") page: Int = 1,
    ): MoviesDto

    @GET("movie/top_rated")
    suspend fun getTopRatedMovieList(
        @Query("language") language: String = "ko",
        @Query("page") page: Int = 1,
    ): MoviesDto

    @GET("search/movie")
    suspend fun getSearchedMovieList(
        @Query("language") language: String = "ko",
        @Query("query") query: String,
    ): MoviesDto

    @GET("movie/{movie_id}")
    suspend fun getMovieDetail(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = "ko",
    ): MovieDetailDto

    @GET("movie/{movie_id}/credits")
    suspend fun getCreditsList(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = "en",
    ): CreditsDto

    // TMDB는 language 하나로 요청하면 그 언어 영상만 주므로,
    // 한국 영화의 한국어 예고편과 외국 영화의 영어 예고편을 함께 받도록 두 언어 요청
    @GET("movie/{movie_id}/videos")
    suspend fun getVideosList(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = "ko",
        @Query("include_video_language") includeVideoLanguage: String = "ko,en",
    ): VideosDto
}
