package com.codealphas.themovie.data.movie

import androidx.paging.LoadState
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import androidx.paging.testing.asSnapshot
import com.codealphas.themovie.data.movie.remote.CreditsDto
import com.codealphas.themovie.data.movie.remote.MovieDetailDto
import com.codealphas.themovie.data.movie.remote.MovieDto
import com.codealphas.themovie.data.movie.remote.MoviesDto
import com.codealphas.themovie.data.movie.remote.TmdbApiService
import com.codealphas.themovie.data.movie.remote.VideosDto
import com.codealphas.themovie.domain.movie.Movie
import com.codealphas.themovie.domain.result.RemoteError
import com.codealphas.themovie.domain.result.RemoteErrorException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

private const val MOVIES_PER_PAGE = 2

class MovieRepositoryImplTest {
    @Test
    fun `getPopularMovies의 첫 페이지를 불러오면 service의 getPopularMovies에 page 1을 넘기고 받은 영화를 돌려줘야 한다`() =
        runTest {
            val service = FakeTmdbApiService()

            val movies = MovieRepositoryImpl(service).getPopularMovies().asSnapshot()

            assertEquals(1, service.popularPages.first())
            assertEquals(listOf(100, 101), movies.take(MOVIES_PER_PAGE).map { it.id })
        }

    @Test
    fun `getTopRatedMovies의 첫 페이지를 불러오면 service의 getTopRatedMovies에 page 1을 넘기고 getPopularMovies는 부르지 않아야 한다`() =
        runTest {
            val service = FakeTmdbApiService()

            MovieRepositoryImpl(service).getTopRatedMovies().asSnapshot()

            assertEquals(1, service.topRatedPages.first())
            assertTrue(service.popularPages.isEmpty())
        }

    @Test
    fun `다음 페이지를 이어 받으면 1페이지의 영화 뒤에 2페이지의 영화가 이어져야 한다`() =
        runTest {
            val service = FakeTmdbApiService(totalPages = 3)

            val movies =
                MovieRepositoryImpl(
                    service,
                ).getPopularMovies().asSnapshot { scrollTo(index = MOVIES_PER_PAGE) }

            assertEquals(listOf(100, 101, 200, 201), movies.take(MOVIES_PER_PAGE * 2).map { it.id })
        }

    @Test
    fun `total_pages가 MAX_PAGES보다 커도 MAX_PAGES페이지까지만 요청해야 한다`() =
        runTest {
            val service = FakeTmdbApiService(totalPages = 500)

            val movies = MovieRepositoryImpl(service).getPopularMovies().asSnapshot { appendScrollWhile { true } }

            assertEquals(MAX_PAGES, service.popularPages.max())
            assertEquals(MAX_PAGES * MOVIES_PER_PAGE, movies.size)
        }

    @Test
    fun `첫 페이지 요청이 실패하면 RemoteError를 담은 LoadState Error가 되어야 한다`() =
        runTest {
            val service = FakeTmdbApiService(failure = IOException())
            val presenter = RecordingPresenter()

            val collecting =
                launch(Dispatchers.Default) {
                    MovieRepositoryImpl(service).getPopularMovies().collectLatest { presenter.collectFrom(it) }
                }
            val error = presenter.loadStateFlow.first { it?.refresh is LoadState.Error }?.refresh as LoadState.Error
            collecting.cancel()

            assertEquals(RemoteError.Network, (error.error as RemoteErrorException).error)
        }

    @Test
    fun `searchMovies의 첫 페이지를 불러오면 service의 searchMovies에 검색어와 page 1을 넘기고 받은 영화를 돌려줘야 한다`() =
        runTest {
            val service = FakeTmdbApiService()

            val movies = MovieRepositoryImpl(service).searchMovies("avatar").asSnapshot()

            assertEquals("avatar" to 1, service.searchRequests.first())
            assertEquals(listOf(100, 101), movies.take(MOVIES_PER_PAGE).map { it.id })
        }

    @Test
    fun `searchMovies의 다음 페이지를 불러오면 service의 searchMovies에 같은 검색어와 page 2를 넘겨야 한다`() =
        runTest {
            val service = FakeTmdbApiService(totalPages = 3)

            MovieRepositoryImpl(service).searchMovies("avatar").asSnapshot { scrollTo(index = MOVIES_PER_PAGE) }

            assertEquals(listOf("avatar" to 1, "avatar" to 2), service.searchRequests.take(2))
        }
}

private class RecordingPresenter : PagingDataPresenter<Movie>(Dispatchers.Unconfined) {
    override suspend fun presentPagingDataEvent(event: PagingDataEvent<Movie>) = Unit
}

private fun movieDto(id: Int): MovieDto =
    MovieDto(
        adult = false,
        backdropPath = null,
        genreIds = emptyList(),
        id = id,
        originalLanguage = "ko",
        originalTitle = "Original",
        overview = "",
        popularity = 1.0,
        posterPath = null,
        releaseDate = "",
        title = "Movie $id",
        video = false,
        voteAverage = 0.0,
        voteCount = 0,
    )

private class FakeTmdbApiService(
    private val totalPages: Int = 1,
    private val failure: Throwable? = null,
) : TmdbApiService {
    val popularPages = mutableListOf<Int>()
    val topRatedPages = mutableListOf<Int>()
    val searchRequests = mutableListOf<Pair<String, Int>>()

    override suspend fun getPopularMovies(
        language: String,
        page: Int,
    ): MoviesDto {
        popularPages += page
        return response(page)
    }

    override suspend fun getTopRatedMovies(
        language: String,
        page: Int,
    ): MoviesDto {
        topRatedPages += page
        return response(page)
    }

    override suspend fun searchMovies(
        language: String,
        query: String,
        page: Int,
    ): MoviesDto {
        searchRequests += query to page
        return response(page)
    }

    override suspend fun getMovieDetail(
        movieId: Int,
        language: String,
    ): MovieDetailDto = error("사용하지 않음")

    override suspend fun getCredits(
        movieId: Int,
        language: String,
    ): CreditsDto = error("사용하지 않음")

    override suspend fun getVideos(
        movieId: Int,
        language: String,
        includeVideoLanguage: String,
    ): VideosDto = error("사용하지 않음")

    private fun response(page: Int): MoviesDto {
        failure?.let { throw it }
        return MoviesDto(
            page = page,
            results = List(MOVIES_PER_PAGE) { movieDto(id = page * 100 + it) },
            totalPages = totalPages,
        )
    }
}
