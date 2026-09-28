package com.codealphas.themovie.data.movie.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class VideosFromServer(
    val id: Int,
    val results: List<VideoItem>,
) // TMDB 서버로부터 받은 영화 비디오 정보

@Serializable
internal data class VideoItem(
    @SerialName("iso_639_1")
    val iso6391: String,
    @SerialName("iso_3166_1")
    val iso31661: String,
    val name: String,
    val key: String,
    val site: String,
    val size: Int,
    val type: String,
    val official: Boolean,
    @SerialName("published_at")
    val publishedAt: String,
    val id: String,
) // TMDB 서버로부터 받은 영화 비디오 상세 정보
