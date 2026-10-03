package com.codealphas.themovie.presentation.movie.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.movie.Movie
import com.codealphas.themovie.domain.movie.MovieDetail
import com.codealphas.themovie.domain.movie.MovieDetailResult
import com.codealphas.themovie.domain.movie.MovieRepository
import com.codealphas.themovie.domain.result.DataResult
import com.codealphas.themovie.domain.result.RemoteError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MovieDetailViewModelTest {
    @Test
    fun `movieId로 상세를 요청하면 그 id를 한 번만 요청하고 영화 정보를 넣어야 한다`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val detail = movieDetail()
            val repository = FakeMovieDetailRepository(MovieDetailResult(detail = detail, errors = emptyList()))
            val viewModel = MovieDetailViewModel(repository, movieIdHandle(detail.id))
            try {
                assertEquals(true, viewModel.state.value.isLoading)
                assertEquals(emptyList<Int>(), repository.requestedIds)
                advanceUntilIdle()

                assertEquals(listOf(detail.id), repository.requestedIds)
                assertEquals(detail, viewModel.state.value.detail)
                assertEquals(false, viewModel.state.value.isLoading)
            } finally {
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `화면이 멈춘 동안 상세 요청이 실패하면 다시 구독할 때 영화 정보 없이 오류를 받아야 한다`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeMovieDetailRepository(
                    MovieDetailResult(detail = null, errors = listOf(RemoteError.Network)),
                )
            val viewModel = MovieDetailViewModel(repository, movieIdHandle(42))
            try {
                advanceUntilIdle()
                val effects = mutableListOf<MovieDetailEffect>()
                backgroundScope.launch { viewModel.effect.collect { effects += it } }
                runCurrent()

                assertEquals(
                    MovieDetailUiState(detail = null, isLoading = false, loadError = RemoteError.Network),
                    viewModel.state.value,
                )
                assertEquals(listOf(MovieDetailEffect.ShowError(RemoteError.Network)), effects)
            } finally {
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `화면이 멈춘 동안 출연진만 불러오지 못하면 상세를 보여 주고 화면으로 돌아왔을 때 오류 안내를 띄워야 한다`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val detail = movieDetail()
            val repository =
                FakeMovieDetailRepository(
                    MovieDetailResult(detail = detail, errors = listOf(RemoteError.Timeout)),
                )
            val viewModel = MovieDetailViewModel(repository, movieIdHandle(detail.id))
            try {
                advanceUntilIdle()
                val effects = mutableListOf<MovieDetailEffect>()
                backgroundScope.launch { viewModel.effect.collect { effects += it } }
                runCurrent()

                assertEquals(detail, viewModel.state.value.detail)
                assertNull(viewModel.state.value.loadError)
                assertEquals(listOf(MovieDetailEffect.ShowError(RemoteError.Timeout)), effects)
            } finally {
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `출연진과 영상을 모두 불러오지 못하면 상세를 보여 주고 오류 안내를 한 번만 띄워야 한다`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val detail = movieDetail()
            val repository =
                FakeMovieDetailRepository(
                    MovieDetailResult(detail = detail, errors = listOf(RemoteError.Timeout, RemoteError.Network)),
                )
            val viewModel = MovieDetailViewModel(repository, movieIdHandle(detail.id))
            try {
                advanceUntilIdle()
                val effects = mutableListOf<MovieDetailEffect>()
                backgroundScope.launch { viewModel.effect.collect { effects += it } }
                runCurrent()

                assertEquals(detail, viewModel.state.value.detail)
                assertEquals(listOf(MovieDetailEffect.ShowError(RemoteError.Timeout)), effects)
            } finally {
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `상세를 불러오지 못한 뒤 다시 시도를 누르면 상세를 한 번 더 불러와 영화 정보를 보여 줘야 한다`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val detail = movieDetail()
            val repository =
                FakeMovieDetailRepository(
                    MovieDetailResult(detail = null, errors = listOf(RemoteError.Network)),
                    MovieDetailResult(detail = detail, errors = emptyList()),
                )
            val viewModel = MovieDetailViewModel(repository, movieIdHandle(detail.id))
            try {
                advanceUntilIdle()
                viewModel.onIntent(MovieDetailIntent.RetryClicked)
                advanceUntilIdle()

                assertEquals(listOf(detail.id, detail.id), repository.requestedIds)
                assertEquals(MovieDetailUiState(detail = detail, isLoading = false), viewModel.state.value)
            } finally {
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `상세를 다시 불러오는 중에 다시 시도를 누르면 추가로 요청하지 않아야 한다`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeMovieDetailRepository(
                    MovieDetailResult(detail = null, errors = listOf(RemoteError.Network)),
                )
            val viewModel = MovieDetailViewModel(repository, movieIdHandle(42))
            try {
                advanceUntilIdle()
                val gate = CompletableDeferred<Unit>()
                repository.gate = gate

                // 다시 요청하는 코루틴이 시작되기 전에 누른 경우와 응답을 기다리는 중에 누른 경우를 모두 확인
                viewModel.onIntent(MovieDetailIntent.RetryClicked)
                viewModel.onIntent(MovieDetailIntent.RetryClicked)
                runCurrent()
                viewModel.onIntent(MovieDetailIntent.RetryClicked)
                runCurrent()

                assertEquals(listOf(42, 42), repository.requestedIds)
                assertEquals(MovieDetailUiState(detail = null, isLoading = true), viewModel.state.value)

                gate.complete(Unit)
                advanceUntilIdle()

                assertEquals(listOf(42, 42), repository.requestedIds)
                assertEquals(
                    MovieDetailUiState(detail = null, isLoading = false, loadError = RemoteError.Network),
                    viewModel.state.value,
                )
            } finally {
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }
}

private fun movieIdHandle(movieId: Int): SavedStateHandle = SavedStateHandle(mapOf("movieId" to movieId))

private fun movieDetail(): MovieDetail =
    MovieDetail(
        id = 42,
        title = "기생충",
        posterUrl = null,
        releaseDate = "2019-05-30",
        overview = "줄거리",
        voteAverage = 8.6,
        cast = emptyList(),
        videos = emptyList(),
    )

// 요청마다 results를 차례로 돌려주고 마지막 결과는 그 뒤 요청에도 반복 적용
private class FakeMovieDetailRepository(
    private vararg val results: MovieDetailResult,
) : MovieRepository {
    val requestedIds = mutableListOf<Int>()
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun getMovieDetail(movieId: Int): MovieDetailResult {
        requestedIds += movieId
        gate?.await()
        return results[minOf(requestedIds.size, results.size) - 1]
    }

    override suspend fun getPopularMovies(): DataResult<List<Movie>> = error("사용하지 않음")

    override suspend fun getTopRatedMovies(): DataResult<List<Movie>> = error("사용하지 않음")

    override suspend fun searchMovies(query: String): DataResult<List<Movie>> = error("사용하지 않음")
}
