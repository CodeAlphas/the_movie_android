package com.example.movieapplication.networks

import com.example.movieapplication.models.CreditsFromServer
import com.example.movieapplication.models.MoviesFromServer
import com.example.movieapplication.models.VideosFromServer
import com.example.movieapplication.utils.Credentials.TMDB_API_KEY
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RetrofitService {

    // 일별 인기 영화 정보 요청
    @GET("movie/popular")
    suspend fun getPopularMovieList(
        @Query("api_key") apiKey: String = TMDB_API_KEY, // TMDB API 키
        @Query("language") language: String = "ko",
        @Query("page") page: Int = 1
    ): MoviesFromServer

    // 최고 평점 영화 정보 요청
    @GET("movie/top_rated")
    suspend fun getTopRatedMovieList(
        @Query("api_key") apiKey: String = TMDB_API_KEY, // TMDB API 키
        @Query("language") language: String = "ko",
        @Query("page") page: Int = 1
    ): MoviesFromServer

    // 사용자 검색 영화 정보 요청
    @GET("search/movie")
    suspend fun getSearchedMovieList(
        @Query("api_key") apiKey: String = TMDB_API_KEY, // TMDB API 키
        @Query("language") language: String = "ko",
        @Query("query") query: String
    ): MoviesFromServer

    // 영화 관계자 정보 요청
    @GET("movie/{movie_id}/credits")
    suspend fun getCreditsList(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String = TMDB_API_KEY, // TMDB API 키
        @Query("language") language: String = "en"
    ): CreditsFromServer

    // 영화 관련 동영상 정보 요청
    @GET("movie/{movie_id}/videos")
    suspend fun getVideosList(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String = TMDB_API_KEY, // TMDB API 키
        @Query("language") language: String = "en"
    ): VideosFromServer

}