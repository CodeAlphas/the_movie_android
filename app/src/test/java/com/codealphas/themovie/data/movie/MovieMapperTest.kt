package com.codealphas.themovie.data.movie

import com.codealphas.themovie.domain.movie.Cast
import com.codealphas.themovie.domain.movie.Movie
import com.codealphas.themovie.domain.movie.Video
import com.codealphas.themovie.models.CreditItem
import com.codealphas.themovie.models.MovieItem
import com.codealphas.themovie.models.VideoItem
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
    fun `영상 응답이면 key만 매핑해야 한다`() {
        assertEquals(Video(key = "abc123"), videoItem(key = "abc123").toVideo())
    }
}

private fun movieItem(
    id: Int = 1,
    title: String = "제목",
    posterPath: String? = "/poster.jpg",
    releaseDate: String = "2024-01-02",
    overview: String = "줄거리",
    voteAverage: Double = 7.5,
): MovieItem =
    MovieItem(
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
): CreditItem =
    CreditItem(
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

private fun videoItem(key: String): VideoItem =
    VideoItem(
        iso6391 = "en",
        iso31661 = "US",
        name = "예고편",
        key = key,
        site = "YouTube",
        size = 1080,
        type = "Trailer",
        official = true,
        publishedAt = "2024-01-02",
        id = "video-id",
    )
