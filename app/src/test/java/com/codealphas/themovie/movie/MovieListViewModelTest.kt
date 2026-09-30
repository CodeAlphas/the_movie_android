package com.codealphas.themovie.movie

import androidx.lifecycle.SavedStateHandle
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
            val viewModel = MovieListViewModel(repository, popularHandle())
            try {
                viewModel.loadMovies()
                viewModel.loadMovies()
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
            val viewModel = MovieListViewModel(repository, popularHandle())
            val collect = launch { viewModel.remoteError.collect {} }
            try {
                viewModel.loadMovies()
                advanceUntilIdle()
                viewModel.loadMovies()
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
            val viewModel = MovieListViewModel(repository, popularHandle())
            try {
                viewModel.loadMovies()
                advanceUntilIdle()
                val errors = mutableListOf<RemoteError>()
                backgroundScope.launch { viewModel.remoteError.collect { errors += it } }
                runCurrent()

                assertEquals(listOf<RemoteError>(RemoteError.Network), errors)
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
            val viewModel = MovieListViewModel(repository, topRatedHandle())
            val collect = launch { viewModel.remoteError.collect {} }
            try {
                viewModel.loadMovies()
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
            val viewModel = MovieListViewModel(repository, popularHandle())
            try {
                viewModel.loadMovies()
                runCurrent()

                assertEquals(MovieListUiState(isLoading = true), viewModel.uiState.value)
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
            val viewModel = MovieListViewModel(repository, popularHandle())
            try {
                viewModel.loadMovies()
                advanceUntilIdle()

                assertEquals(MovieListUiState(movies = movies), viewModel.uiState.value)
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
            val viewModel = MovieListViewModel(repository, popularHandle())
            try {
                viewModel.loadMovies()
                advanceUntilIdle()

                // 실패를 빈 목록으로 채우면 탭이 다시 보일 때 재요청하지 않으므로, movies가 null로 남는지 확인
                assertEquals(MovieListUiState(loadError = RemoteError.Network), viewModel.uiState.value)
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
            val viewModel = MovieListViewModel(repository, popularHandle())
            try {
                viewModel.loadMovies()
                advanceUntilIdle()
                repository.hangPopular = true
                viewModel.loadMovies()
                runCurrent()

                assertEquals(MovieListUiState(isLoading = true), viewModel.uiState.value)
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

private fun popularHandle(): SavedStateHandle =
    SavedStateHandle(mapOf(MovieListViewModel.ARG_CATEGORY to MovieCategory.POPULAR))

private fun topRatedHandle(): SavedStateHandle =
    SavedStateHandle(mapOf(MovieListViewModel.ARG_CATEGORY to MovieCategory.TOP_RATED))

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
