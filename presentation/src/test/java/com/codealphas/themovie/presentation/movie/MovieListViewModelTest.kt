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
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MovieListViewModelTest {
    @Test
    fun `인기 영화 요청이 진행 중일 때 다시 요청하면 Repository를 한 번만 호출해야 한다`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeMovieRepository(hangPopular = true)
            val viewModel = MovieListViewModel(repository, MovieCategory.POPULAR)
            try {
                viewModel.onIntent(MovieListIntent.PageShown)
                viewModel.onIntent(MovieListIntent.PageShown)
                advanceUntilIdle()

                assertEquals(1, repository.popularCalls)
            } finally {
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `인기 영화 요청이 실패한 뒤 다시 요청하면 Repository를 한 번 더 호출해야 한다`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeMovieRepository(hangPopular = false)
            val viewModel = MovieListViewModel(repository, MovieCategory.POPULAR)
            val collect = launch { viewModel.effect.collect {} }
            try {
                viewModel.onIntent(MovieListIntent.PageShown)
                advanceUntilIdle()
                viewModel.onIntent(MovieListIntent.PageShown)
                advanceUntilIdle()

                assertEquals(2, repository.popularCalls)
            } finally {
                collect.cancel()
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `화면이 멈춘 동안 인기 영화 요청이 실패하면 다시 구독할 때 오류를 받아야 한다`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeMovieRepository(hangPopular = false)
            val viewModel = MovieListViewModel(repository, MovieCategory.POPULAR)
            try {
                viewModel.onIntent(MovieListIntent.PageShown)
                advanceUntilIdle()
                val effects = mutableListOf<MovieListEffect>()
                backgroundScope.launch { viewModel.effect.collect { effects += it } }
                runCurrent()

                assertEquals(listOf<MovieListEffect>(MovieListEffect.ShowError(RemoteError.Network)), effects)
            } finally {
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `평점 분류로 요청하면 평점 영화만 한 번 요청해야 한다`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeMovieRepository(hangPopular = false)
            val viewModel = MovieListViewModel(repository, MovieCategory.TOP_RATED)
            val collect = launch { viewModel.effect.collect {} }
            try {
                viewModel.onIntent(MovieListIntent.PageShown)
                advanceUntilIdle()

                assertEquals(0, repository.popularCalls)
                assertEquals(1, repository.topRatedCalls)
            } finally {
                collect.cancel()
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `인기 영화를 요청하는 동안 로딩 중이어야 한다`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeMovieRepository(hangPopular = true)
            val viewModel = MovieListViewModel(repository, MovieCategory.POPULAR)
            try {
                viewModel.onIntent(MovieListIntent.PageShown)
                runCurrent()

                assertEquals(MovieListUiState(isLoading = true), viewModel.state.value)
            } finally {
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `인기 영화 요청이 성공하면 로딩을 끝내고 영화 목록을 보여 줘야 한다`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val movies = listOf(listMovie("popular"))
            val repository = FakeMovieRepository(popularResult = Outcome.Success(movies))
            val viewModel = MovieListViewModel(repository, MovieCategory.POPULAR)
            try {
                viewModel.onIntent(MovieListIntent.PageShown)
                advanceUntilIdle()

                assertEquals(MovieListUiState(movies = movies), viewModel.state.value)
            } finally {
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `인기 영화 요청이 실패하면 영화 목록을 보여 주지 않고 오류 상태가 되어야 한다`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeMovieRepository()
            val viewModel = MovieListViewModel(repository, MovieCategory.POPULAR)
            try {
                viewModel.onIntent(MovieListIntent.PageShown)
                advanceUntilIdle()

                // 실패를 빈 목록으로 채우면 화면이 다시 보일 때 오는 PageShown에서 다시 요청하지 않으므로, movies가 null로 남는지 확인
                assertEquals(MovieListUiState(loadError = RemoteError.Network), viewModel.state.value)
            } finally {
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `인기 영화 요청이 실패한 뒤 다시 요청하면 오류 상태에서 로딩 중으로 바뀌어야 한다`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeMovieRepository()
            val viewModel = MovieListViewModel(repository, MovieCategory.POPULAR)
            try {
                viewModel.onIntent(MovieListIntent.PageShown)
                advanceUntilIdle()
                repository.hangPopular = true
                viewModel.onIntent(MovieListIntent.PageShown)
                runCurrent()

                assertEquals(MovieListUiState(isLoading = true), viewModel.state.value)
            } finally {
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `인기 영화 요청이 실패한 뒤 다시 시도를 누르면 Repository를 한 번 더 호출해야 한다`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeMovieRepository()
            val viewModel = MovieListViewModel(repository, MovieCategory.POPULAR)
            val collect = launch { viewModel.effect.collect {} }
            try {
                viewModel.onIntent(MovieListIntent.PageShown)
                advanceUntilIdle()
                viewModel.onIntent(MovieListIntent.RetryClicked)
                advanceUntilIdle()

                assertEquals(2, repository.popularCalls)
            } finally {
                collect.cancel()
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `인기 영화를 받은 뒤 탭이 다시 보이면 Repository를 다시 호출하지 않아야 한다`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeMovieRepository(popularResult = Outcome.Success(listOf(listMovie("popular"))))
            val viewModel = MovieListViewModel(repository, MovieCategory.POPULAR)
            try {
                viewModel.onIntent(MovieListIntent.PageShown)
                advanceUntilIdle()
                viewModel.onIntent(MovieListIntent.PageShown)
                advanceUntilIdle()

                assertEquals(1, repository.popularCalls)
            } finally {
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }
}

private fun listMovie(title: String): Movie =
    Movie(
        id = title.hashCode(),
        title = title,
        posterUrl = null,
        releaseDate = "",
        overview = "",
        voteAverage = 0.0,
    )

private class FakeMovieRepository(
    var hangPopular: Boolean = false,
    private val popularResult: DataResult<List<Movie>> = Outcome.Failure(RemoteError.Network),
) : MovieRepository {
    var popularCalls: Int = 0
    var topRatedCalls: Int = 0

    override suspend fun getPopularMovies(): DataResult<List<Movie>> {
        popularCalls += 1
        if (hangPopular) awaitCancellation()
        return popularResult
    }

    override suspend fun getTopRatedMovies(): DataResult<List<Movie>> {
        topRatedCalls += 1
        return Outcome.Failure(RemoteError.Network)
    }

    override suspend fun searchMovies(query: String): DataResult<List<Movie>> = error("사용하지 않음")

    override suspend fun getMovieDetail(movieId: Int): MovieDetailResult = error("사용하지 않음")
}
