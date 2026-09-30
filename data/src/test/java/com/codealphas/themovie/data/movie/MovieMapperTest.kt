package com.codealphas.themovie.data.movie

import com.codealphas.themovie.data.movie.remote.CreditDto
import com.codealphas.themovie.data.movie.remote.CreditsDto
import com.codealphas.themovie.data.movie.remote.MovieDetailDto
import com.codealphas.themovie.data.movie.remote.MovieDto
import com.codealphas.themovie.data.movie.remote.VideoDto
import com.codealphas.themovie.domain.movie.Cast
import com.codealphas.themovie.domain.movie.Movie
import com.codealphas.themovie.domain.movie.MovieDetail
import com.codealphas.themovie.domain.movie.Video
import com.codealphas.themovie.domain.result.DataResult
import com.codealphas.themovie.domain.result.RemoteError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MovieMapperTest {
    @Test
    fun `posterPath가 null이면 posterUrl이 null이어야 한다`() {
        assertNull(movieItem(posterPath = null).toMovie().posterUrl)
    }

    @Test
    fun `posterPath가 있으면 posterUrl에 w500이 한 번만 붙어야 한다`() {
        assertEquals(
            "https://image.tmdb.org/t/p/w500/poster.jpg",
            movieItem(posterPath = "/poster.jpg").toMovie().posterUrl,
        )
    }

    @Test
    fun `영화 응답이면 화면이 쓰는 필드로 매핑해야 한다`() {
        val movie =
            movieItem(
                id = 42,
                title = "기생충",
                posterPath = "/parasite.jpg",
                releaseDate = "2019-05-30",
                overview = "줄거리",
                voteAverage = 8.6,
            ).toMovie()

        assertEquals(
            Movie(
                id = 42,
                title = "기생충",
                posterUrl = "https://image.tmdb.org/t/p/w500/parasite.jpg",
                releaseDate = "2019-05-30",
                overview = "줄거리",
                voteAverage = 8.6,
            ),
            movie,
        )
    }

    @Test
    fun `profilePath가 null이면 profileUrl이 null이어야 한다`() {
        assertNull(creditItem(profilePath = null).toCast().profileUrl)
    }

    @Test
    fun `profilePath가 있으면 profileUrl에 w500이 한 번만 붙어야 한다`() {
        assertEquals(
            "https://image.tmdb.org/t/p/w500/profile.jpg",
            creditItem(profilePath = "/profile.jpg").toCast().profileUrl,
        )
    }

    @Test
    fun `출연진 응답이면 화면이 쓰는 필드로 매핑해야 한다`() {
        val cast =
            creditItem(
                name = "송강호",
                character = "기택",
                profilePath = "/song.jpg",
            ).toCast()

        assertEquals(
            Cast(
                name = "송강호",
                character = "기택",
                profileUrl = "https://image.tmdb.org/t/p/w500/song.jpg",
            ),
            cast,
        )
    }

    @Test
    fun `영상 응답이면 key, 이름, 종류를 매핑해야 한다`() {
        assertEquals(
            Video(key = "abc123", name = "예고편", type = "Trailer"),
            videoItem(key = "abc123").toVideo(),
        )
    }

    @Test
    fun `YouTube가 아닌 사이트 영상이면 목록에서 제외해야 한다`() {
        val videos =
            listOf(
                videoItem(key = "vimeo", site = "Vimeo"),
                videoItem(key = "youtube"),
            ).toYouTubeVideos()

        assertEquals(listOf("youtube"), videos.map(Video::key))
    }

    @Test
    fun `영상 종류가 섞여 있으면 Trailer, Teaser, 나머지 순으로 정렬해야 한다`() {
        val videos =
            listOf(
                videoItem(key = "clip", type = "Clip"),
                videoItem(key = "teaser", type = "Teaser"),
                videoItem(key = "featurette", type = "Featurette"),
                videoItem(key = "trailer", type = "Trailer"),
            ).toYouTubeVideos()

        assertEquals(listOf("trailer", "teaser", "clip", "featurette"), videos.map(Video::key))
    }

    @Test
    fun `같은 종류 안에서는 공식 영상이 먼저 와야 한다`() {
        val videos =
            listOf(
                videoItem(key = "fan", official = false),
                videoItem(key = "official", official = true),
            ).toYouTubeVideos()

        assertEquals(listOf("official", "fan"), videos.map(Video::key))
    }

    @Test
    fun `같은 종류와 공식 여부 안에서는 한국어 영상이 먼저 와야 한다`() {
        val videos =
            listOf(
                videoItem(key = "english", language = "en"),
                videoItem(key = "korean", language = "ko"),
            ).toYouTubeVideos()

        assertEquals(listOf("korean", "english"), videos.map(Video::key))
    }

    @Test
    fun `상세 posterPath가 null이면 posterUrl이 null이어야 한다`() {
        val detail =
            movieDetail(posterPath = null).toMovieDetail(
                cast = emptyList(),
                videos = emptyList(),
            )

        assertNull(detail.posterUrl)
    }

    @Test
    fun `상세 응답이면 포스터, 출연진, 영상을 한 영화 정보로 매핑해야 한다`() {
        val detail =
            movieDetail(
                id = 42,
                title = "기생충",
                posterPath = "/parasite.jpg",
                releaseDate = "2019-05-30",
                overview = "줄거리",
                voteAverage = 8.6,
            ).toMovieDetail(
                cast = listOf(creditItem(name = "송강호", character = "기택", profilePath = "/song.jpg")),
                videos = listOf(videoItem(key = "abc123")),
            )

        assertEquals(
            MovieDetail(
                id = 42,
                title = "기생충",
                posterUrl = "https://image.tmdb.org/t/p/w500/parasite.jpg",
                releaseDate = "2019-05-30",
                overview = "줄거리",
                voteAverage = 8.6,
                cast =
                    listOf(
                        Cast(
                            name = "송강호",
                            character = "기택",
                            profileUrl = "https://image.tmdb.org/t/p/w500/song.jpg",
                        ),
                    ),
                videos = listOf(Video(key = "abc123", name = "예고편", type = "Trailer")),
            ),
            detail,
        )
    }

    @Test
    fun `상세 본문이 실패하면 영화 정보 없이 그 오류만 반환해야 한다`() {
        val result =
            toMovieDetailResult(
                detail = DataResult.Failure(RemoteError.Network),
                cast = DataResult.Success(CreditsDto(id = 1, cast = listOf(creditItem()))),
                videos = DataResult.Failure(RemoteError.Timeout),
            )

        assertNull(result.detail)
        assertEquals(listOf(RemoteError.Network), result.errors)
    }

    @Test
    fun `출연진과 영상이 실패하면 빈 목록과 두 오류를 반환해야 한다`() {
        val result =
            toMovieDetailResult(
                detail = DataResult.Success(movieDetail(id = 7, title = "제목")),
                cast = DataResult.Failure(RemoteError.Network),
                videos = DataResult.Failure(RemoteError.Timeout),
            )

        assertEquals(
            MovieDetail(
                id = 7,
                title = "제목",
                posterUrl = "https://image.tmdb.org/t/p/w500/poster.jpg",
                releaseDate = "2024-01-02",
                overview = "줄거리",
                voteAverage = 7.5,
                cast = emptyList(),
                videos = emptyList(),
            ),
            result.detail,
        )
        assertEquals(listOf(RemoteError.Network, RemoteError.Timeout), result.errors)
    }
}

private fun movieItem(
    id: Int = 1,
    title: String = "제목",
    posterPath: String? = "/poster.jpg",
    releaseDate: String = "2024-01-02",
    overview: String = "줄거리",
    voteAverage: Double = 7.5,
): MovieDto =
    MovieDto(
        adult = false,
        backdropPath = "/backdrop.jpg",
        genreIds = emptyList(),
        id = id,
        originalLanguage = "ko",
        originalTitle = "Original",
        overview = overview,
        popularity = 1.0,
        posterPath = posterPath,
        releaseDate = releaseDate,
        title = title,
        video = false,
        voteAverage = voteAverage,
        voteCount = 10,
    )

private fun creditItem(
    name: String = "이름",
    character: String = "배역",
    profilePath: String? = "/profile.jpg",
): CreditDto =
    CreditDto(
        adult = false,
        gender = null,
        id = 1,
        knownForDepartment = "Acting",
        name = name,
        originalName = "Original",
        popularity = 1.0,
        profilePath = profilePath,
        castId = 2,
        character = character,
        creditId = "credit",
        order = 0,
    )

private fun movieDetail(
    id: Int = 1,
    title: String = "제목",
    posterPath: String? = "/poster.jpg",
    releaseDate: String = "2024-01-02",
    overview: String = "줄거리",
    voteAverage: Double = 7.5,
): MovieDetailDto =
    MovieDetailDto(
        id = id,
        title = title,
        posterPath = posterPath,
        releaseDate = releaseDate,
        overview = overview,
        voteAverage = voteAverage,
    )

private fun videoItem(
    key: String,
    site: String = "YouTube",
    type: String = "Trailer",
    official: Boolean = true,
    language: String = "en",
): VideoDto =
    VideoDto(
        iso6391 = language,
        iso31661 = "US",
        name = "예고편",
        key = key,
        site = site,
        size = 1080,
        type = type,
        official = official,
        publishedAt = "2024-01-02",
        id = "video-id",
    )
