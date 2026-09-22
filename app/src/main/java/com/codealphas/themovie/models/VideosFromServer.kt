package com.example.movieapplication.models

data class VideosFromServer(
    val id: Int,
    val results: ArrayList<VideoItem>
) // TMDB 서버로부터 받은 영화 비디오 정보

data class VideoItem(
    val iso_639_1: String,
    val iso_3166_1: String,
    val name: String,
    val key: String,
    val site: String,
    val size: Int,
    val type: String,
    val official: Boolean,
    val published_at: String,
    val id: String
) // TMDB 서버로부터 받은 영화 비디오 상세 정보