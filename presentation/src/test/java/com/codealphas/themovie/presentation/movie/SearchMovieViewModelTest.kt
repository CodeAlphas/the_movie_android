package com.codealphas.themovie.presentation.movie

import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.movie.Movie
import com.codealphas.themovie.domain.movie.MovieDetailResult
import com.codealphas.themovie.domain.movie.MovieRepository
import com.codealphas.themovie.domain.result.DataResult
import com.codealphas.themovie.domain.result.Outcome
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
    fun `검색어를 입력하면 검색 요청 전에 입력창 검색어가 바로 바뀌어야 한다`() {
        val repository = FakeSearchMovieRepository()
        runSearchTest(repository) { viewModel ->
            viewModel.onIntent(SearchMovieIntent.QueryChanged("a"))
            assertEquals("a", viewModel.state.value.query)
            assertEquals(0, repository.queries.size)
        }
    }

    @Test
    fun `300ms 안에 검색어가 연달아 바뀌면 마지막 검색어만 한 번 요청해야 한다`() {
        val repository = FakeSearchMovieRepository()
        runSearchTest(repository) { viewModel ->
            viewModel.onIntent(SearchMovieIntent.QueryChanged("a"))
            advance(BETWEEN_KEYSTROKES_MS)
            assertEquals(0, repository.queries.size)
            viewModel.onIntent(SearchMovieIntent.QueryChanged("ab"))
            advance(BETWEEN_KEYSTROKES_MS)
            assertEquals(0, repository.queries.size)
            viewModel.onIntent(SearchMovieIntent.QueryChanged("abc"))
            advance(DEBOUNCE_MS - 1)
            assertEquals(0, repository.queries.size)
            advance(1)
            assertEquals(listOf("abc"), repository.queries)
        }
    }

    @Test
    fun `앞 검색이 끝나기 전에 검색어가 바뀌면 두 번 요청하고 나중 검색어의 영화 목록만 보여 줘야 한다`() {
        val first = listOf(movie("first"))
        val second = listOf(movie("second"))
        val repository =
            FakeSearchMovieRepository(
                slowQueries = setOf("first"),
                results =
                    mapOf(
                        "first" to Outcome.Success(first),
                        "second" to Outcome.Success(second),
                    ),
            )
        runSearchTest(repository) { viewModel ->
            viewModel.onIntent(SearchMovieIntent.QueryChanged("first"))
            advance(DEBOUNCE_MS)
            assertEquals(listOf("first"), repository.queries)
            viewModel.onIntent(SearchMovieIntent.QueryChanged("second"))
            advance(DEBOUNCE_MS)
            assertEquals(listOf("first", "second"), repository.queries)
            assertEquals(second, viewModel.state.value.movies)
            advance(SLOW_SEARCH_MS)
            assertEquals(second, viewModel.state.value.movies)
        }
    }

    @Test
    fun `결과가 있을 때 공백만 입력하면 추가 요청 없이 이전 목록이 남아야 한다`() {
        val movies = listOf(movie("avatar"))
        val repository =
            FakeSearchMovieRepository(
                results = mapOf("avatar" to Outcome.Success(movies)),
            )
        runSearchTest(repository) { viewModel ->
            viewModel.onIntent(SearchMovieIntent.QueryChanged("avatar"))
            advance(DEBOUNCE_MS)
            viewModel.onIntent(SearchMovieIntent.QueryChanged("   "))
            advance(DEBOUNCE_MS)
            assertEquals(listOf("avatar"), repository.queries)
            assertEquals(movies, viewModel.state.value.movies)
        }
    }

    @Test
    fun `앞뒤 공백만 다른 같은 검색어를 넣으면 한 번만 요청해야 한다`() {
        val repository = FakeSearchMovieRepository()
        runSearchTest(repository) { viewModel ->
            viewModel.onIntent(SearchMovieIntent.QueryChanged("avatar"))
            advance(DEBOUNCE_MS)
            viewModel.onIntent(SearchMovieIntent.QueryChanged(" avatar "))
            advance(DEBOUNCE_MS)
            assertEquals(listOf("avatar"), repository.queries)
        }
    }

    @Test
    fun `같은 검색어를 이어서 넣으면 한 번만 요청해야 한다`() {
        val repository = FakeSearchMovieRepository()
        runSearchTest(repository) { viewModel ->
            viewModel.onIntent(SearchMovieIntent.QueryChanged("avatar"))
            advance(DEBOUNCE_MS)
            viewModel.onIntent(SearchMovieIntent.QueryChanged("avatar"))
            advance(DEBOUNCE_MS)
            assertEquals(listOf("avatar"), repository.queries)
        }
    }

    @Test
    fun `검색어를 지웠다가 같은 검색어를 다시 입력하면 한 번 더 요청해야 한다`() {
        val repository = FakeSearchMovieRepository()
        runSearchTest(repository) { viewModel ->
            viewModel.onIntent(SearchMovieIntent.QueryChanged("avatar"))
            advance(DEBOUNCE_MS)
            viewModel.onIntent(SearchMovieIntent.QueryChanged(""))
            advance(DEBOUNCE_MS)
            viewModel.onIntent(SearchMovieIntent.QueryChanged("avatar"))
            advance(DEBOUNCE_MS)
            assertEquals(listOf("avatar", "avatar"), repository.queries)
        }
    }

    @Test
    fun `검색이 실패한 뒤 다른 검색어를 입력하면 오류를 알리고 새 검색어의 영화 목록을 보여 줘야 한다`() {
        val good = listOf(movie("good"))
        val repository =
            FakeSearchMovieRepository(
                results =
                    mapOf(
                        "bad" to Outcome.Failure(RemoteError.Network),
                        "good" to Outcome.Success(good),
                    ),
            )
        runSearchTest(repository) { viewModel ->
            val effects = mutableListOf<SearchMovieEffect>()
            val collect = launch { viewModel.effect.collect { effects += it } }
            try {
                runCurrent()
                viewModel.onIntent(SearchMovieIntent.QueryChanged("bad"))
                advance(DEBOUNCE_MS)
                viewModel.onIntent(SearchMovieIntent.QueryChanged("good"))
                advance(DEBOUNCE_MS)
                assertEquals(listOf<SearchMovieEffect>(SearchMovieEffect.ShowError(RemoteError.Network)), effects)
                assertEquals(listOf("bad", "good"), repository.queries)
                assertEquals(good, viewModel.state.value.movies)
            } finally {
                collect.cancel()
            }
        }
    }

    @Test
    fun `화면이 멈춘 동안 검색이 실패하면 다시 구독할 때 오류를 받아야 한다`() {
        val repository =
            FakeSearchMovieRepository(
                results = mapOf("bad" to Outcome.Failure(RemoteError.Network)),
            )
        runSearchTest(repository) { viewModel ->
            viewModel.onIntent(SearchMovieIntent.QueryChanged("bad"))
            advance(DEBOUNCE_MS)
            val effects = mutableListOf<SearchMovieEffect>()
            backgroundScope.launch { viewModel.effect.collect { effects += it } }
            runCurrent()

            assertEquals(listOf<SearchMovieEffect>(SearchMovieEffect.ShowError(RemoteError.Network)), effects)
        }
    }

    @Test
    fun `검색 결과가 0건이면 로딩을 끝내고 빈 영화 목록을 보여 줘야 한다`() {
        val repository =
            FakeSearchMovieRepository(
                results = mapOf("zzz" to Outcome.Success(emptyList())),
            )
        runSearchTest(repository) { viewModel ->
            viewModel.onIntent(SearchMovieIntent.QueryChanged("zzz"))
            advance(DEBOUNCE_MS)
            assertEquals(SearchMovieUiState(query = "zzz", movies = emptyList()), viewModel.state.value)
        }
    }

    @Test
    fun `앞 검색이 끝나기 전에 검색어가 바뀌면 나중 검색어 응답이 올 때까지 로딩 중이어야 한다`() {
        val second = listOf(movie("second"))
        val repository =
            FakeSearchMovieRepository(
                slowQueries = setOf("first", "second"),
                results = mapOf("second" to Outcome.Success(second)),
            )
        runSearchTest(repository) { viewModel ->
            viewModel.onIntent(SearchMovieIntent.QueryChanged("first"))
            advance(DEBOUNCE_MS)
            assertEquals(true, viewModel.state.value.isLoading)
            viewModel.onIntent(SearchMovieIntent.QueryChanged("second"))
            advance(DEBOUNCE_MS)
            assertEquals(true, viewModel.state.value.isLoading)
            advance(SLOW_SEARCH_MS - 1)
            assertEquals(true, viewModel.state.value.isLoading)
            advance(1)
            assertEquals(SearchMovieUiState(query = "second", movies = second), viewModel.state.value)
        }
    }

    @Test
    fun `검색이 실패하면 앞 검색어의 영화 목록을 그대로 보여 주고 오류 상태가 되어야 한다`() {
        val movies = listOf(movie("avatar"))
        val repository =
            FakeSearchMovieRepository(
                results =
                    mapOf(
                        "avatar" to Outcome.Success(movies),
                        "bad" to Outcome.Failure(RemoteError.Network),
                    ),
            )
        runSearchTest(repository) { viewModel ->
            viewModel.onIntent(SearchMovieIntent.QueryChanged("avatar"))
            advance(DEBOUNCE_MS)
            viewModel.onIntent(SearchMovieIntent.QueryChanged("bad"))
            advance(DEBOUNCE_MS)
            assertEquals(
                SearchMovieUiState(query = "bad", movies = movies, loadError = RemoteError.Network),
                viewModel.state.value,
            )
        }
    }

    @Test
    fun `검색이 실패한 뒤 다른 검색어를 입력하면 오류 상태에서 로딩 중으로 바뀌어야 한다`() {
        val repository =
            FakeSearchMovieRepository(
                slowQueries = setOf("good"),
                results = mapOf("bad" to Outcome.Failure(RemoteError.Network)),
            )
        runSearchTest(repository) { viewModel ->
            viewModel.onIntent(SearchMovieIntent.QueryChanged("bad"))
            advance(DEBOUNCE_MS)
            viewModel.onIntent(SearchMovieIntent.QueryChanged("good"))
            advance(DEBOUNCE_MS)
            assertEquals(SearchMovieUiState(query = "good", isLoading = true), viewModel.state.value)
        }
    }

    @Test
    fun `검색이 실패한 뒤 다시 시도를 누르면 같은 검색어로 다시 요청해 영화 목록을 보여 줘야 한다`() {
        val repository = FakeSearchMovieRepository(failOnceQueries = setOf("avatar"))
        runSearchTest(repository) { viewModel ->
            viewModel.onIntent(SearchMovieIntent.QueryChanged("avatar"))
            advance(DEBOUNCE_MS)
            assertEquals(RemoteError.Network, viewModel.state.value.loadError)
            viewModel.onIntent(SearchMovieIntent.RetryClicked)
            runCurrent()
            assertEquals(listOf("avatar", "avatar"), repository.queries)
            assertEquals(
                SearchMovieUiState(query = "avatar", movies = listOf(movie("avatar"))),
                viewModel.state.value,
            )
        }
    }

    @Test
    fun `검색을 다시 하는 중에 다시 시도를 누르면 추가로 요청하지 않아야 한다`() {
        val repository =
            FakeSearchMovieRepository(
                slowQueries = setOf("avatar"),
                failOnceQueries = setOf("avatar"),
            )
        runSearchTest(repository) { viewModel ->
            viewModel.onIntent(SearchMovieIntent.QueryChanged("avatar"))
            advance(DEBOUNCE_MS + SLOW_SEARCH_MS)
            // 다시 검색하는 코루틴이 시작되기 전에 두 번 누르는 경우와 응답을 기다리는 중에 누르는 경우를 함께 확인
            viewModel.onIntent(SearchMovieIntent.RetryClicked)
            viewModel.onIntent(SearchMovieIntent.RetryClicked)
            runCurrent()
            viewModel.onIntent(SearchMovieIntent.RetryClicked)
            advance(SLOW_SEARCH_MS)
            assertEquals(listOf("avatar", "avatar"), repository.queries)
            assertEquals(
                SearchMovieUiState(query = "avatar", movies = listOf(movie("avatar"))),
                viewModel.state.value,
            )
        }
    }

    @Test
    fun `검색이 성공한 상태에서 다시 시도를 누르면 추가로 요청하지 않아야 한다`() {
        val repository = FakeSearchMovieRepository()
        runSearchTest(repository) { viewModel ->
            viewModel.onIntent(SearchMovieIntent.QueryChanged("avatar"))
            advance(DEBOUNCE_MS)
            viewModel.onIntent(SearchMovieIntent.RetryClicked)
            runCurrent()
            assertEquals(listOf("avatar"), repository.queries)
        }
    }

    @Test
    fun `검색이 실패한 뒤 검색어를 모두 지우고 다시 시도를 누르면 추가로 요청하지 않아야 한다`() {
        val repository = FakeSearchMovieRepository(failOnceQueries = setOf("avatar"))
        runSearchTest(repository) { viewModel ->
            viewModel.onIntent(SearchMovieIntent.QueryChanged("avatar"))
            advance(DEBOUNCE_MS)
            viewModel.onIntent(SearchMovieIntent.QueryChanged(""))
            advance(DEBOUNCE_MS)
            viewModel.onIntent(SearchMovieIntent.RetryClicked)
            runCurrent()
            assertEquals(listOf("avatar"), repository.queries)
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
        // 검색 요청이 진행 중일 때 취소하면 mapLatest의 취소 마무리가 Main에 예약되는데,
        // resetMain 뒤에 실행되면 Main이 없어 테스트가 실패하므로 resetMain 전에 예약된 작업 실행
        runCurrent()
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
    private val failOnceQueries: Set<String> = emptySet(),
) : MovieRepository {
    val queries = mutableListOf<String>()

    override suspend fun searchMovies(query: String): DataResult<List<Movie>> {
        val isFirstRequest = query !in queries
        queries += query
        if (query in slowQueries) delay(SLOW_SEARCH_MS)
        if (query in failOnceQueries && isFirstRequest) return Outcome.Failure(RemoteError.Network)
        return results[query] ?: Outcome.Success(listOf(movie(query)))
    }

    override suspend fun getPopularMovies(): DataResult<List<Movie>> = error("사용하지 않음")

    override suspend fun getTopRatedMovies(): DataResult<List<Movie>> = error("사용하지 않음")

    override suspend fun getMovieDetail(movieId: Int): MovieDetailResult = error("사용하지 않음")
}
