package com.codealphas.themovie.viewmodels

import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.models.CreditsFromServer
import com.codealphas.themovie.models.MoviesFromServer
import com.codealphas.themovie.models.VideosFromServer
import com.codealphas.themovie.networks.TmdbApiService
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
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class MovieListLoadTest {
    @Test
    fun `인기 영화 요청이 진행 중일 때 다시 요청하면 서버를 한 번만 호출해야 한다`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val service = FakeTmdbApiService(hangPopular = true)
            val viewModel = MovieViewModel(service)
            try {
                viewModel.makePopMovieListApiCall()
                viewModel.makePopMovieListApiCall()
                advanceUntilIdle()

                assertEquals(1, service.popularCalls)
            } finally {
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `인기 영화 요청이 실패한 뒤 다시 요청하면 서버를 한 번 더 호출해야 한다`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val service = FakeTmdbApiService(hangPopular = false)
            val viewModel = MovieViewModel(service)
            val collect = launch { viewModel.remoteError.collect {} }
            try {
                viewModel.makePopMovieListApiCall()
                advanceUntilIdle()
                viewModel.makePopMovieListApiCall()
                advanceUntilIdle()

                assertEquals(2, service.popularCalls)
            } finally {
                collect.cancel()
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }
}

private class FakeTmdbApiService(
    private val hangPopular: Boolean,
) : TmdbApiService {
    var popularCalls: Int = 0

    override suspend fun getPopularMovieList(
        language: String,
        page: Int,
    ): MoviesFromServer {
        popularCalls += 1
        if (hangPopular) awaitCancellation()
        throw IOException()
    }

    override suspend fun getTopRatedMovieList(
        language: String,
        page: Int,
    ): MoviesFromServer = error("사용하지 않음")

    override suspend fun getSearchedMovieList(
        language: String,
        query: String,
    ): MoviesFromServer = error("사용하지 않음")

    override suspend fun getCreditsList(
        movieId: Int,
        language: String,
    ): CreditsFromServer = error("사용하지 않음")

    override suspend fun getVideosList(
        movieId: Int,
        language: String,
    ): VideosFromServer = error("사용하지 않음")
}
