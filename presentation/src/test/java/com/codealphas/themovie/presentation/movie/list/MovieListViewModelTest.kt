package com.codealphas.themovie.presentation.movie.list

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.testing.asSnapshot
import com.codealphas.themovie.domain.movie.Movie
import com.codealphas.themovie.domain.movie.MovieDetailResult
import com.codealphas.themovie.domain.movie.MoviePage
import com.codealphas.themovie.domain.movie.MovieRepository
import com.codealphas.themovie.domain.result.DataResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MovieListViewModelTest {
    @Test
    fun `인기 분류면 인기 영화 목록을 돌려줘야 한다`() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val repository = FakeMovieRepository()
            val viewModel = MovieListViewModel(repository, MovieCategory.POPULAR)
            try {
                val titles = viewModel.movies.asSnapshot().map { it.title }

                assertEquals(listOf("popular"), titles)
                assertEquals(0, repository.topRatedCollections)
            } finally {
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `높은 평점 분류면 평점 높은 영화 목록을 돌려줘야 한다`() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val repository = FakeMovieRepository()
            val viewModel = MovieListViewModel(repository, MovieCategory.TOP_RATED)
            try {
                val titles = viewModel.movies.asSnapshot().map { it.title }

                assertEquals(listOf("topRated"), titles)
                assertEquals(0, repository.popularCollections)
            } finally {
                viewModel.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `movies를 두 번 수집해도 Repository의 Flow는 한 번만 수집해야 한다`() =
        runTest {
            Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
            val repository = FakeMovieRepository()
            val viewModel = MovieListViewModel(repository, MovieCategory.POPULAR)
            try {
                viewModel.movies.asSnapshot()
                viewModel.movies.asSnapshot()

                assertEquals(1, repository.popularCollections)
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

// PagingData.from은 cachedIn과 함께 쓰면 asSnapshot이 끝나지 않으므로, 실제 Pager 적용
private class StaticPagingSource(
    private val movies: List<Movie>,
) : PagingSource<Int, Movie>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Movie> =
        LoadResult.Page(data = movies, prevKey = null, nextKey = null)

    override fun getRefreshKey(state: PagingState<Int, Movie>): Int? = null
}

private fun pagerOf(movie: Movie): Flow<PagingData<Movie>> =
    Pager(PagingConfig(pageSize = 20)) { StaticPagingSource(listOf(movie)) }.flow

private class FakeMovieRepository : MovieRepository {
    var popularCollections: Int = 0
    var topRatedCollections: Int = 0

    override fun getPopularMovies(): Flow<PagingData<Movie>> =
        flow {
            popularCollections += 1
            emitAll(pagerOf(listMovie("popular")))
        }

    override fun getTopRatedMovies(): Flow<PagingData<Movie>> =
        flow {
            topRatedCollections += 1
            emitAll(pagerOf(listMovie("topRated")))
        }

    override suspend fun searchMovies(
        query: String,
        page: Int,
    ): DataResult<MoviePage> = error("사용하지 않음")

    override suspend fun getMovieDetail(movieId: Int): MovieDetailResult = error("사용하지 않음")
}
