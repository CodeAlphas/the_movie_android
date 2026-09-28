package com.codealphas.themovie.movie

import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.movie.Cast
import com.codealphas.themovie.domain.movie.Movie
import com.codealphas.themovie.domain.movie.MovieRepository
import com.codealphas.themovie.domain.movie.Video
import com.codealphas.themovie.domain.result.DataResult
import com.codealphas.themovie.domain.result.RemoteError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MovieListLoadTest {
    @Test
    fun `인기 영화 요청이 진행 중일 때 다시 요청하면 Repository를 한 번만 호출해야 한다`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeMovieRepository(hangPopular = true)
            val viewModel = MovieViewModel(repository)
            try {
                viewModel.makePopMovieListApiCall()
                viewModel.makePopMovieListApiCall()
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
            val viewModel = MovieViewModel(repository)
            val collect = launch { viewModel.remoteError.collect {} }
            try {
                viewModel.makePopMovieListApiCall()
                advanceUntilIdle()
                viewModel.makePopMovieListApiCall()
                advanceUntilIdle()

                assertEquals(2, repository.popularCalls)
            } finally {
                collect.cancel()
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }
}

private class FakeMovieRepository(
    private val hangPopular: Boolean,
) : MovieRepository {
    var popularCalls: Int = 0

    override suspend fun getPopularMovies(): DataResult<List<Movie>> {
        popularCalls += 1
        if (hangPopular) awaitCancellation()
        return DataResult.Failure(RemoteError.Network)
    }

    override suspend fun getTopRatedMovies(): DataResult<List<Movie>> = error("사용하지 않음")

    override suspend fun searchMovies(query: String): DataResult<List<Movie>> = error("사용하지 않음")

    override suspend fun getVideos(movieId: Int): DataResult<List<Video>> = error("사용하지 않음")

    override suspend fun getCast(movieId: Int): DataResult<List<Cast>> = error("사용하지 않음")
}
