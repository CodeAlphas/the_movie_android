package com.codealphas.themovie.data.movie.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

internal interface TmdbApiService {
    // 일별 인기 영화 정보 요청
    @GET("movie/popular")
    suspend fun getPopularMovieList(
        @Query("language") language: String = "ko",
        @Query("page") page: Int = 1,
    ): MoviesDto

    // 최고 평점 영화 정보 요청
    @GET("movie/top_rated")
    suspend fun getTopRatedMovieList(
        @Query("language") language: String = "ko",
        @Query("page") page: Int = 1,
    ): MoviesDto

    // 사용자 검색 영화 정보 요청
    @GET("search/movie")
    suspend fun getSearchedMovieList(
        @Query("language") language: String = "ko",
        @Query("query") query: String,
    ): MoviesDto

    // 영화 관계자 정보 요청
    @GET("movie/{movie_id}/credits")
    suspend fun getCreditsList(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = "en",
    ): CreditsDto

    // 영화 관련 동영상 정보 요청
    @GET("movie/{movie_id}/videos")
    suspend fun getVideosList(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = "en",
    ): VideosDto
}
