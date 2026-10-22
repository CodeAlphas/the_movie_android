package com.codealphas.themovie.data.movie

import com.codealphas.themovie.data.movie.remote.CreditsDto
import com.codealphas.themovie.data.movie.remote.MovieDetailDto
import com.codealphas.themovie.data.movie.remote.MovieDto
import com.codealphas.themovie.data.movie.remote.MoviesDto
import com.codealphas.themovie.data.movie.remote.TmdbApiService
import com.codealphas.themovie.data.movie.remote.VideosDto
import com.codealphas.themovie.domain.result.Outcome
import com.codealphas.themovie.domain.result.RemoteError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class MovieRepositoryImplTest {
    @Test
    fun `인기 영화를 요청하면 넘긴 페이지로 요청하고 페이지 정보를 돌려줘야 한다`() =
        runTest {
            val service = FakeTmdbApiService(totalPages = 8)

            val result = MovieRepositoryImpl(service).getPopularMovies(page = 2)

            assertEquals(listOf(2), service.popularPages)
            val page = (result as Outcome.Success).data
            assertEquals(2, page.page)
            assertEquals(8, page.totalPages)
        }

    @Test
    fun `평점 높은 영화를 요청하면 넘긴 페이지로 요청해야 한다`() =
        runTest {
            val service = FakeTmdbApiService()

            MovieRepositoryImpl(service).getTopRatedMovies(page = 3)

            assertEquals(listOf(3), service.topRatedPages)
        }

    @Test
    fun `영화를 검색하면 검색어와 넘긴 페이지로 요청해야 한다`() =
        runTest {
            val service = FakeTmdbApiService()

            MovieRepositoryImpl(service).searchMovies(query = "avatar", page = 4)

            assertEquals(listOf("avatar" to 4), service.searchRequests)
        }

    @Test
    fun `목록 요청이 네트워크 오류로 실패하면 오류를 돌려줘야 한다`() =
        runTest {
            val service = FakeTmdbApiService(failure = IOException())

            val result = MovieRepositoryImpl(service).getPopularMovies(page = 1)

            assertEquals(Outcome.Failure(RemoteError.Network), result)
        }
}

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
        return MoviesDto(page = page, results = emptyList<MovieDto>(), totalPages = totalPages)
    }
}
