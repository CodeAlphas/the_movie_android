package com.codealphas.themovie.data.movie

import com.codealphas.themovie.data.movie.remote.CreditDto
import com.codealphas.themovie.data.movie.remote.CreditsDto
import com.codealphas.themovie.data.movie.remote.MovieDetailDto
import com.codealphas.themovie.data.movie.remote.MovieDto
import com.codealphas.themovie.data.movie.remote.VideoDto
import com.codealphas.themovie.data.movie.remote.VideosDto
import com.codealphas.themovie.domain.movie.Cast
import com.codealphas.themovie.domain.movie.Movie
import com.codealphas.themovie.domain.movie.MovieDetail
import com.codealphas.themovie.domain.movie.MovieDetailResult
import com.codealphas.themovie.domain.movie.Video
import com.codealphas.themovie.domain.result.DataResult
import com.codealphas.themovie.domain.result.RemoteError

private const val TMDB_W500_IMAGE_URL = "https://image.tmdb.org/t/p/w500"

// poster_path, profile_path가 null이면 w500null을 요청하므로, 경로가 있을 때만 이미지 주소를 붙이도록 처리
private fun tmdbImageUrl(path: String?): String? = path?.let { "$TMDB_W500_IMAGE_URL$it" }

internal fun MovieDto.toMovie(): Movie =
    Movie(
        id = id,
        title = title,
        posterUrl = tmdbImageUrl(posterPath),
        releaseDate = releaseDate,
        overview = overview,
        voteAverage = voteAverage,
    )

internal fun CreditDto.toCast(): Cast =
    Cast(
        name = name,
        character = character,
        profileUrl = tmdbImageUrl(profilePath),
    )

internal fun VideoDto.toVideo(): Video = Video(key = key, name = name, type = type)

private const val YOUTUBE_SITE = "YouTube"
private const val KOREAN_LANGUAGE = "ko"
private val VIDEO_TYPE_ORDER = listOf("Trailer", "Teaser")

// VIDEO_TYPE_ORDER에 없는 종류는 뒤로 보내도록 목록 크기를 순위로 반환
private fun videoTypeRank(type: String): Int =
    VIDEO_TYPE_ORDER.indexOf(type).takeIf { it >= 0 } ?: VIDEO_TYPE_ORDER.size

// 플레이어는 YouTube 영상만 재생할 수 있으므로, Vimeo 등 다른 사이트 영상은 제외하고 첫 영상이 예고편이 되도록 정렬
internal fun List<VideoDto>.toYouTubeVideos(): List<Video> =
    filter { it.site == YOUTUBE_SITE }
        .sortedWith(
            compareBy<VideoDto> { videoTypeRank(it.type) }
                .thenByDescending { it.official }
                .thenByDescending { it.iso6391 == KOREAN_LANGUAGE },
        ).map(VideoDto::toVideo)

internal fun MovieDetailDto.toMovieDetail(
    cast: List<CreditDto>,
    videos: List<VideoDto>,
): MovieDetail =
    MovieDetail(
        id = id,
        title = title,
        posterUrl = tmdbImageUrl(posterPath),
        releaseDate = releaseDate,
        overview = overview,
        voteAverage = voteAverage,
        cast = cast.map(CreditDto::toCast),
        videos = videos.toYouTubeVideos(),
    )

internal fun toMovieDetailResult(
    detail: DataResult<MovieDetailDto>,
    cast: DataResult<CreditsDto>,
    videos: DataResult<VideosDto>,
): MovieDetailResult {
    // 상세 본문이 없으면 제목과 포스터를 그릴 수 없으므로, 출연진·영상 오류는 버리고 본문 오류만 반환
    val detailDto =
        when (detail) {
            is DataResult.Failure -> return MovieDetailResult(detail = null, errors = listOf(detail.error))
            is DataResult.Success -> detail.data
        }
    // 본문은 있는데 출연진이나 영상만 실패하면 그 목록을 비워도 제목은 남으므로, 빈 목록과 그 오류를 같이 반환
    val errors = mutableListOf<RemoteError>()
    val castItems =
        when (cast) {
            is DataResult.Failure -> {
                errors += cast.error
                emptyList()
            }
            is DataResult.Success -> cast.data.cast
        }
    val videoItems =
        when (videos) {
            is DataResult.Failure -> {
                errors += videos.error
                emptyList()
            }
            is DataResult.Success -> videos.data.results
        }
    return MovieDetailResult(
        detail = detailDto.toMovieDetail(cast = castItems, videos = videoItems),
        errors = errors,
    )
}
