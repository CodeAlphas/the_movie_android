package com.codealphas.themovie.models

import com.google.gson.annotations.SerializedName

data class MoviesFromServer(
    val page: Int,
    val results: ArrayList<MovieItem>,
) // TMDB 서버로부터 받은 영화 정보

data class MovieItem(
    val adult: Boolean,
    @SerializedName("backdrop_path")
    val backdropPath: String,
    @SerializedName("genre_ids")
    val genreIds: IntArray,
    val id: Int,
    @SerializedName("original_language")
    val originalLanguage: String,
    @SerializedName("original_title")
    val originalTitle: String,
    val overview: String,
    val popularity: Double,
    @SerializedName("poster_path")
    val posterPath: String,
    @SerializedName("release_date")
    val releaseDate: String,
    val title: String,
    val video: Boolean,
    @SerializedName("vote_average")
    val voteAverage: Double,
    @SerializedName("vote_count")
    val voteCount: Int,
) // TMDB 서버로부터 받은 영화 상세 정보
