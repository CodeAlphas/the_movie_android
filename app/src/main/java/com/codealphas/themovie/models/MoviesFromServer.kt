package com.example.movieapplication.models

data class MoviesFromServer(
    val page: Int,
    val results: ArrayList<MovieItem>
) // TMDB 서버로부터 받은 영화 정보

data class MovieItem(
    val adult: Boolean,
    val backdrop_path: String,
    val genre_ids: IntArray,
    val id: Int,
    val original_language: String,
    val original_title: String,
    val overview: String,
    val popularity: Double,
    val poster_path: String,
    val release_date: String,
    val title: String,
    val video: Boolean,
    val vote_average: Double,
    val vote_count: Int
) // TMDB 서버로부터 받은 영화 상세 정보