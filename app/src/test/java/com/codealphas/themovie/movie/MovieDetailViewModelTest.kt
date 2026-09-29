package com.codealphas.themovie.movie

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.movie.Movie
import com.codealphas.themovie.domain.movie.MovieDetail
import com.codealphas.themovie.domain.movie.MovieDetailResult
import com.codealphas.themovie.domain.movie.MovieRepository
import com.codealphas.themovie.domain.result.DataResult
import com.codealphas.themovie.domain.result.RemoteError
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
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
                assertEquals(true, viewModel.uiState.value.isLoading)
                assertEquals(emptyList<Int>(), repository.requestedIds)
                advanceUntilIdle()

                assertEquals(listOf(detail.id), repository.requestedIds)
                assertEquals(detail, viewModel.uiState.value.detail)
                assertEquals(false, viewModel.uiState.value.isLoading)
            } finally {
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `상세 요청이 실패하면 영화 정보 없이 오류를 전달해야 한다`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository =
                FakeMovieDetailRepository(
                    MovieDetailResult(detail = null, errors = listOf(RemoteError.Network)),
                )
            val viewModel = MovieDetailViewModel(repository, movieIdHandle(42))
            val errors = mutableListOf<RemoteError>()
            // replay가 0이라 구독 전에 emit하면 오류가 버려지므로, 요청이 돌기 전에 구독
            val collect =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    viewModel.remoteError.collect { errors += it }
                }
            try {
                advanceUntilIdle()

                assertNull(viewModel.uiState.value.detail)
                assertEquals(false, viewModel.uiState.value.isLoading)
                assertEquals(listOf(RemoteError.Network), errors)
            } finally {
                collect.cancel()
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `출연진 오류가 있어도 영화 정보가 있으면 상세를 보여주고 오류를 전달해야 한다`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val detail = movieDetail()
            val repository =
                FakeMovieDetailRepository(
                    MovieDetailResult(detail = detail, errors = listOf(RemoteError.Timeout)),
                )
            val viewModel = MovieDetailViewModel(repository, movieIdHandle(detail.id))
            val errors = mutableListOf<RemoteError>()
            // replay가 0이라 구독 전에 emit하면 오류가 버려지므로, 요청이 돌기 전에 구독
            val collect =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    viewModel.remoteError.collect { errors += it }
                }
            try {
                advanceUntilIdle()

                assertEquals(detail, viewModel.uiState.value.detail)
                assertEquals(listOf(RemoteError.Timeout), errors)
            } finally {
                collect.cancel()
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }
}

private fun movieIdHandle(movieId: Int): SavedStateHandle =
    SavedStateHandle(mapOf(MovieDetailViewModel.ARG_MOVIE_ID to movieId))

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

private class FakeMovieDetailRepository(
    private val result: MovieDetailResult,
) : MovieRepository {
    val requestedIds = mutableListOf<Int>()

    override suspend fun getMovieDetail(movieId: Int): MovieDetailResult {
        requestedIds += movieId
        return result
    }

    override suspend fun getPopularMovies(): DataResult<List<Movie>> = error("사용하지 않음")

    override suspend fun getTopRatedMovies(): DataResult<List<Movie>> = error("사용하지 않음")

    override suspend fun searchMovies(query: String): DataResult<List<Movie>> = error("사용하지 않음")
}
