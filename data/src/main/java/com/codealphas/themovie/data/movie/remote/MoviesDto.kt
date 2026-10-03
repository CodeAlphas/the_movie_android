package com.codealphas.themovie.data.movie.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MoviesDto(
    val page: Int,
    val results: List<MovieDto>,
)

@Serializable
internal data class MovieDto(
    val adult: Boolean,
    // TMDB는 이미지가 없는 영화에 backdrop_path, poster_path를 null로 주므로, 응답 전체가 파싱에 실패하지 않도록 두 경로를 nullable로 선언
    @SerialName("backdrop_path")
    val backdropPath: String?,
    @SerialName("genre_ids")
    val genreIds: List<Int>,
    val id: Int,
    @SerialName("original_language")
    val originalLanguage: String,
    @SerialName("original_title")
    val originalTitle: String,
    val overview: String,
    val popularity: Double,
    @SerialName("poster_path")
    val posterPath: String?,
    @SerialName("release_date")
    val releaseDate: String,
    val title: String,
    val video: Boolean,
    @SerialName("vote_average")
    val voteAverage: Double,
    @SerialName("vote_count")
    val voteCount: Int,
)
