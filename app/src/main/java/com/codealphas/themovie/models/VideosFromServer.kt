package com.codealphas.themovie.models

import com.google.gson.annotations.SerializedName

data class VideosFromServer(
    val id: Int,
    val results: ArrayList<VideoItem>,
) // TMDB 서버로부터 받은 영화 비디오 정보

data class VideoItem(
    @SerializedName("iso_639_1")
    val iso6391: String,
    @SerializedName("iso_3166_1")
    val iso31661: String,
    val name: String,
    val key: String,
    val site: String,
    val size: Int,
    val type: String,
    val official: Boolean,
    @SerializedName("published_at")
    val publishedAt: String,
    val id: String,
) // TMDB 서버로부터 받은 영화 비디오 상세 정보
