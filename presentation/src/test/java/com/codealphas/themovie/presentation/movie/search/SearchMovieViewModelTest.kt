package com.codealphas.themovie.presentation.movie.search

import androidx.lifecycle.viewModelScope
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingDataEvent
import androidx.paging.PagingDataPresenter
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.codealphas.themovie.domain.movie.Movie
import com.codealphas.themovie.domain.movie.MovieDetailResult
import com.codealphas.themovie.domain.movie.MovieRepository
import com.codealphas.themovie.domain.result.RemoteError
import com.codealphas.themovie.domain.result.RemoteErrorException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchMovieViewModelTest {
    @Test
    fun `QueryChanged하면 검색 요청 전에 query가 바로 바뀌어야 한다`() {
        val repository = FakeSearchMovieRepository()
        runSearchTest(repository) { viewModel, _ ->
            viewModel.onIntent(SearchMovieIntent.QueryChanged("a"))
            assertEquals("a", viewModel.state.value.query)
            assertEquals(0, repository.queries.size)
        }
    }

    @Test
    fun `300ms 안에 query가 연달아 바뀌면 마지막 query만 한 번 요청해야 한다`() {
        val repository = FakeSearchMovieRepository()
        runSearchTest(repository) { viewModel, _ ->
            viewModel.onIntent(SearchMovieIntent.QueryChanged("a"))
            advance(BETWEEN_KEYSTROKES_MS)
            viewModel.onIntent(SearchMovieIntent.QueryChanged("ab"))
            advance(BETWEEN_KEYSTROKES_MS)
            viewModel.onIntent(SearchMovieIntent.QueryChanged("abc"))
            advance(DEBOUNCE_MS - 1)
            assertEquals(0, repository.queries.size)
            advance(1)
            assertEquals(listOf("abc"), repository.queries)
        }
    }

    @Test
    fun `query가 바뀌면 이전 검색어의 영화 대신 새 검색어의 영화를 돌려줘야 한다`() {
        val repository = FakeSearchMovieRepository()
        runSearchTest(repository) { viewModel, presenter ->
            viewModel.onIntent(SearchMovieIntent.QueryChanged("first"))
            advance(DEBOUNCE_MS)
            presenter.awaitTitles(listOf("first"))
            viewModel.onIntent(SearchMovieIntent.QueryChanged("second"))
            advance(DEBOUNCE_MS)
            presenter.awaitTitles(listOf("second"))

            assertEquals(listOf("first", "second"), repository.queries)
        }
    }

    @Test
    fun `query를 공백으로 바꾸면 추가 요청 없이 빈 목록이 되어야 한다`() {
        val repository = FakeSearchMovieRepository()
        runSearchTest(repository) { viewModel, presenter ->
            viewModel.onIntent(SearchMovieIntent.QueryChanged("avatar"))
            advance(DEBOUNCE_MS)
            presenter.awaitTitles(listOf("avatar"))
            viewModel.onIntent(SearchMovieIntent.QueryChanged("   "))
            advance(DEBOUNCE_MS)
            presenter.awaitTitles(emptyList())

            assertEquals(listOf("avatar"), repository.queries)
        }
    }

    @Test
    fun `앞뒤 공백만 다른 같은 query를 넣으면 한 번만 요청해야 한다`() {
        val repository = FakeSearchMovieRepository()
        runSearchTest(repository) { viewModel, _ ->
            viewModel.onIntent(SearchMovieIntent.QueryChanged("avatar"))
            advance(DEBOUNCE_MS)
            viewModel.onIntent(SearchMovieIntent.QueryChanged(" avatar "))
            advance(DEBOUNCE_MS)
            assertEquals(listOf("avatar"), repository.queries)
        }
    }

    @Test
    fun `같은 query를 이어서 넣으면 한 번만 요청해야 한다`() {
        val repository = FakeSearchMovieRepository()
        runSearchTest(repository) { viewModel, _ ->
            viewModel.onIntent(SearchMovieIntent.QueryChanged("avatar"))
            advance(DEBOUNCE_MS)
            viewModel.onIntent(SearchMovieIntent.QueryChanged("avatar"))
            advance(DEBOUNCE_MS)
            assertEquals(listOf("avatar"), repository.queries)
        }
    }

    @Test
    fun `query를 지웠다가 같은 query를 다시 입력하면 한 번 더 요청해야 한다`() {
        val repository = FakeSearchMovieRepository()
        runSearchTest(repository) { viewModel, _ ->
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
    fun `검색 전에는 refresh가 NotLoading이고 append가 끝에 닿지 않아야 한다`() {
        runSearchTest(FakeSearchMovieRepository()) { viewModel, presenter ->
            advance(DEBOUNCE_MS)
            val states = presenter.awaitLoadStates { it.refresh is LoadState.NotLoading }

            assertEquals(LoadState.NotLoading(endOfPaginationReached = false), states.append)
        }
    }

    @Test
    fun `검색이 실패하면 RemoteError를 담은 RemoteErrorException으로 refresh가 Error가 되어야 한다`() {
        val repository = FakeSearchMovieRepository(failQueries = setOf("bad"))
        runSearchTest(repository) { viewModel, presenter ->
            viewModel.onIntent(SearchMovieIntent.QueryChanged("bad"))
            advance(DEBOUNCE_MS)
            val error = presenter.awaitLoadStates { it.refresh is LoadState.Error }.refresh as LoadState.Error

            assertEquals(RemoteError.Network, (error.error as RemoteErrorException).error)
        }
    }

    @Test
    fun `검색이 실패한 뒤 query를 모두 지우면 refresh가 Error에서 벗어나야 한다`() {
        val repository = FakeSearchMovieRepository(failQueries = setOf("bad"))
        runSearchTest(repository) { viewModel, presenter ->
            viewModel.onIntent(SearchMovieIntent.QueryChanged("bad"))
            advance(DEBOUNCE_MS)
            presenter.awaitLoadStates { it.refresh is LoadState.Error }
            viewModel.onIntent(SearchMovieIntent.QueryChanged(""))
            advance(DEBOUNCE_MS)
            val states = presenter.awaitLoadStates { it.refresh is LoadState.NotLoading }

            assertTrue(states.refresh is LoadState.NotLoading)
            assertEquals(listOf("bad"), repository.queries)
        }
    }

    @Test
    fun `검색 결과가 0건이면 refresh가 NotLoading이고 append가 끝에 닿아야 한다`() {
        val repository = FakeSearchMovieRepository(results = mapOf("zzz" to emptyList()))
        runSearchTest(repository) { viewModel, presenter ->
            viewModel.onIntent(SearchMovieIntent.QueryChanged("zzz"))
            advance(DEBOUNCE_MS)
            val states =
                presenter.awaitLoadStates {
                    it.refresh is LoadState.NotLoading && it.append.endOfPaginationReached
                }

            assertTrue(states.append.endOfPaginationReached)
        }
    }
}

private const val DEBOUNCE_MS = 300L
private const val BETWEEN_KEYSTROKES_MS = 100L

@OptIn(ExperimentalCoroutinesApi::class)
private fun TestScope.advance(millis: Long) {
    advanceTimeBy(millis)
    runCurrent()
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun runSearchTest(
    repository: FakeSearchMovieRepository,
    body: suspend TestScope.(SearchMovieViewModel, RecordingPresenter) -> Unit,
) = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    val viewModel = SearchMovieViewModel(repository)
    val presenter = RecordingPresenter()
    try {
        // cachedIn은 수집하는 쪽이 있어야 위쪽 흐름이 시작되므로, 화면처럼 항상 수집
        collectMovies(viewModel, presenter)
        runCurrent()
        body(viewModel, presenter)
    } finally {
        viewModel.viewModelScope.cancel()
        // 진행 중인 작업을 취소하면 마무리가 Main에 예약되는데, resetMain 뒤에 실행되면 Main이 없어 테스트가 실패하므로
        // resetMain 전에 예약된 작업 실행
        runCurrent()
        Dispatchers.resetMain()
    }
}

private fun TestScope.collectMovies(
    viewModel: SearchMovieViewModel,
    presenter: RecordingPresenter,
) {
    backgroundScope.launch { viewModel.movies.collectLatest { presenter.collectFrom(it) } }
    runCurrent()
}

// 화면의 LazyPagingItems처럼 PagingData를 받아 목록과 로드 상태를 기록하는 PagingDataPresenter
private class RecordingPresenter : PagingDataPresenter<Movie>(Dispatchers.Unconfined) {
    override suspend fun presentPagingDataEvent(event: PagingDataEvent<Movie>) = Unit

    suspend fun awaitTitles(expected: List<String>) {
        loadStateFlow.first { snapshot().items.map { it.title } == expected && it?.refresh is LoadState.NotLoading }
    }

    suspend fun awaitLoadStates(
        predicate: (androidx.paging.CombinedLoadStates) -> Boolean,
    ): androidx.paging.CombinedLoadStates = checkNotNull(loadStateFlow.first { it != null && predicate(it) })
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

// 검색어마다 돌려줄 영화나 실패를 정하고, 요청된 검색어를 순서대로 기록하는 Fake
private class FakeSearchMovieRepository(
    private val results: Map<String, List<Movie>> = emptyMap(),
    private val failQueries: Set<String> = emptySet(),
) : MovieRepository {
    val queries = mutableListOf<String>()

    override fun searchMovies(query: String): Flow<PagingData<Movie>> {
        queries += query
        return Pager(PagingConfig(pageSize = 20)) {
            SearchPagingSource(
                movies = results[query] ?: listOf(movie(query)),
                fails = query in failQueries,
            )
        }.flow
    }

    override fun getPopularMovies(): Flow<PagingData<Movie>> = error("사용하지 않음")

    override fun getTopRatedMovies(): Flow<PagingData<Movie>> = error("사용하지 않음")

    override suspend fun getMovieDetail(movieId: Int): MovieDetailResult = error("사용하지 않음")
}

private class SearchPagingSource(
    private val movies: List<Movie>,
    private val fails: Boolean,
) : PagingSource<Int, Movie>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Movie> =
        if (fails) {
            LoadResult.Error(RemoteErrorException(RemoteError.Network))
        } else {
            LoadResult.Page(data = movies, prevKey = null, nextKey = null)
        }

    override fun getRefreshKey(state: PagingState<Int, Movie>): Int? = null
}
