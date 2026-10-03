package com.codealphas.themovie.data.movie.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MovieDetailDto(
    val id: Int,
    val title: String,
    // TMDB는 포스터가 없는 영화에 poster_path를 null로 주므로, 상세 응답 전체가 파싱에 실패하지 않도록 nullable로 선언
    @SerialName("poster_path")
    val posterPath: String?,
    @SerialName("release_date")
    val releaseDate: String,
    val overview: String,
    @SerialName("vote_average")
    val voteAverage: Double,
)
