package com.codealphas.themovie.data.movie

import com.codealphas.themovie.domain.movie.Cast
import com.codealphas.themovie.domain.movie.Movie
import com.codealphas.themovie.domain.movie.Video
import com.codealphas.themovie.models.CreditItem
import com.codealphas.themovie.models.MovieItem
import com.codealphas.themovie.models.VideoItem

private const val TMDB_W500_IMAGE_URL = "https://image.tmdb.org/t/p/w500"

// poster_path, profile_path가 null이면 w500null을 요청하므로, 경로가 있을 때만 이미지 주소를 붙이도록 처리
private fun tmdbImageUrl(path: String?): String? = path?.let { "$TMDB_W500_IMAGE_URL$it" }

fun MovieItem.toMovie(): Movie =
    Movie(
        id = id,
        title = title,
        posterUrl = tmdbImageUrl(posterPath),
        releaseDate = releaseDate,
        overview = overview,
        voteAverage = voteAverage,
    )

fun CreditItem.toCast(): Cast =
    Cast(
        name = name,
        character = character,
        profileUrl = tmdbImageUrl(profilePath),
    )

fun VideoItem.toVideo(): Video = Video(key = key)
