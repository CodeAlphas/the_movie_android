package com.codealphas.themovie.movie

import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.movie.Movie
import com.codealphas.themovie.domain.movie.MovieDetailResult
import com.codealphas.themovie.domain.movie.MovieRepository
import com.codealphas.themovie.domain.result.DataResult
import com.codealphas.themovie.domain.result.RemoteError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchMovieViewModelTest {
    @Test
    fun `300ms 안에 검색어가 연달아 바뀌면 마지막 검색어만 한 번 요청해야 한다`() {
        val repository = FakeSearchMovieRepository()
        runSearchTest(repository) { viewModel ->
            viewModel.onQueryChange("a")
            advance(BETWEEN_KEYSTROKES_MS)
            assertEquals(0, repository.queries.size)
            viewModel.onQueryChange("ab")
            advance(BETWEEN_KEYSTROKES_MS)
            assertEquals(0, repository.queries.size)
            viewModel.onQueryChange("abc")
            advance(DEBOUNCE_MS - 1)
            assertEquals(0, repository.queries.size)
            advance(1)
            assertEquals(listOf("abc"), repository.queries)
        }
    }

    @Test
    fun `앞 검색이 끝나기 전에 검색어가 바뀌면 두 번 요청하고 나중 결과만 목록에 남아야 한다`() {
        val first = listOf(movie("first"))
        val second = listOf(movie("second"))
        val repository =
            FakeSearchMovieRepository(
                slowQueries = setOf("first"),
                results =
                    mapOf(
                        "first" to DataResult.Success(first),
                        "second" to DataResult.Success(second),
                    ),
            )
        runSearchTest(repository) { viewModel ->
            viewModel.onQueryChange("first")
            advance(DEBOUNCE_MS)
            assertEquals(listOf("first"), repository.queries)
            viewModel.onQueryChange("second")
            advance(DEBOUNCE_MS)
            assertEquals(listOf("first", "second"), repository.queries)
            assertEquals(second, viewModel.uiState.value.movies)
            advance(SLOW_SEARCH_MS)
            assertEquals(second, viewModel.uiState.value.movies)
        }
    }

    @Test
    fun `결과가 있을 때 공백만 입력하면 추가 요청 없이 이전 목록이 남아야 한다`() {
        val movies = listOf(movie("avatar"))
        val repository =
            FakeSearchMovieRepository(
                results = mapOf("avatar" to DataResult.Success(movies)),
            )
        runSearchTest(repository) { viewModel ->
            viewModel.onQueryChange("avatar")
            advance(DEBOUNCE_MS)
            viewModel.onQueryChange("   ")
            advance(DEBOUNCE_MS)
            assertEquals(listOf("avatar"), repository.queries)
            assertEquals(movies, viewModel.uiState.value.movies)
        }
    }

    @Test
    fun `앞뒤 공백만 다른 같은 검색어를 넣으면 한 번만 요청해야 한다`() {
        val repository = FakeSearchMovieRepository()
        runSearchTest(repository) { viewModel ->
            viewModel.onQueryChange("avatar")
            advance(DEBOUNCE_MS)
            viewModel.onQueryChange(" avatar ")
            advance(DEBOUNCE_MS)
            assertEquals(listOf("avatar"), repository.queries)
        }
    }

    @Test
    fun `같은 검색어를 이어서 넣으면 한 번만 요청해야 한다`() {
        val repository = FakeSearchMovieRepository()
        runSearchTest(repository) { viewModel ->
            viewModel.onQueryChange("avatar")
            advance(DEBOUNCE_MS)
            viewModel.onQueryChange("avatar")
            advance(DEBOUNCE_MS)
            assertEquals(listOf("avatar"), repository.queries)
        }
    }

    @Test
    fun `검색어를 지웠다가 같은 검색어를 다시 입력하면 한 번 더 요청해야 한다`() {
        val repository = FakeSearchMovieRepository()
        runSearchTest(repository) { viewModel ->
            viewModel.onQueryChange("avatar")
            advance(DEBOUNCE_MS)
            viewModel.onQueryChange("")
            advance(DEBOUNCE_MS)
            viewModel.onQueryChange("avatar")
            advance(DEBOUNCE_MS)
            assertEquals(listOf("avatar", "avatar"), repository.queries)
        }
    }

    @Test
    fun `검색이 실패한 뒤 다른 검색어를 입력하면 오류를 알리고 새 결과를 목록에 넣어야 한다`() {
        val good = listOf(movie("good"))
        val repository =
            FakeSearchMovieRepository(
                results =
                    mapOf(
                        "bad" to DataResult.Failure(RemoteError.Network),
                        "good" to DataResult.Success(good),
                    ),
            )
        runSearchTest(repository) { viewModel ->
            val errors = mutableListOf<RemoteError>()
            val collect = launch { viewModel.remoteError.collect { errors += it } }
            try {
                runCurrent()
                viewModel.onQueryChange("bad")
                advance(DEBOUNCE_MS)
                viewModel.onQueryChange("good")
                advance(DEBOUNCE_MS)
                assertEquals(listOf(RemoteError.Network), errors)
                assertEquals(listOf("bad", "good"), repository.queries)
                assertEquals(good, viewModel.uiState.value.movies)
            } finally {
                collect.cancel()
            }
        }
    }
}

private const val DEBOUNCE_MS = 300L
private const val BETWEEN_KEYSTROKES_MS = 100L
private const val SLOW_SEARCH_MS = 10_000L

@OptIn(ExperimentalCoroutinesApi::class)
private fun TestScope.advance(millis: Long) {
    advanceTimeBy(millis)
    runCurrent()
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun runSearchTest(
    repository: FakeSearchMovieRepository,
    body: suspend TestScope.(SearchMovieViewModel) -> Unit,
) = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    val viewModel = SearchMovieViewModel(repository)
    try {
        runCurrent()
        body(viewModel)
    } finally {
        viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
    }
}

private fun movie(title: String): Movie =
    Movie(
        id = title.hashCode(),
        title = title,
        posterUrl = null,
        releaseDate = "",
        overview = "",
        voteAverage = 0.0,
    )

private class FakeSearchMovieRepository(
    private val slowQueries: Set<String> = emptySet(),
    private val results: Map<String, DataResult<List<Movie>>> = emptyMap(),
) : MovieRepository {
    val queries = mutableListOf<String>()

    override suspend fun searchMovies(query: String): DataResult<List<Movie>> {
        queries += query
        if (query in slowQueries) delay(SLOW_SEARCH_MS)
        return results[query] ?: DataResult.Success(listOf(movie(query)))
    }

    override suspend fun getPopularMovies(): DataResult<List<Movie>> = error("사용하지 않음")

    override suspend fun getTopRatedMovies(): DataResult<List<Movie>> = error("사용하지 않음")

    override suspend fun getMovieDetail(movieId: Int): MovieDetailResult = error("사용하지 않음")
}
