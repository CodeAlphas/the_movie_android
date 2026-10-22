package com.codealphas.themovie.data.movie

import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.testing.TestPager
import com.codealphas.themovie.domain.movie.Movie
import com.codealphas.themovie.domain.result.DataResult
import com.codealphas.themovie.domain.result.Outcome
import com.codealphas.themovie.domain.result.RemoteError
import com.codealphas.themovie.domain.result.RemoteErrorException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoviePagingSourceTest {
    @Test
    fun `refresh하면 1페이지의 영화를 돌려주고 prevKey는 null, nextKey는 2여야 한다`() =
        runTest {
            val source = MoviePagingSource(fakePages(totalPages = 3, 1 to listOf(1, 2), 2 to listOf(3)))

            val result = TestPager(MoviePagingConfig, source).refresh() as PagingSource.LoadResult.Page

            assertEquals(listOf(1, 2), result.data.map { it.id })
            assertNull(result.prevKey)
            assertEquals(2, result.nextKey)
        }

    @Test
    fun `append하면 1페이지의 영화 뒤에 2페이지의 영화가 이어져야 한다`() =
        runTest {
            val source = MoviePagingSource(fakePages(totalPages = 3, 1 to listOf(1, 2), 2 to listOf(3, 4)))
            val pager = TestPager(MoviePagingConfig, source)

            pager.refresh()
            pager.append()

            assertEquals(listOf(1, 2, 3, 4), pager.getPages().flatMap { page -> page.data.map { it.id } })
        }

    @Test
    fun `2페이지를 받으면 prevKey가 1이어야 한다`() =
        runTest {
            val source = MoviePagingSource(fakePages(totalPages = 3, 1 to listOf(1), 2 to listOf(2)))
            val pager = TestPager(MoviePagingConfig, source)

            pager.refresh()
            val second = pager.append() as PagingSource.LoadResult.Page

            assertEquals(1, second.prevKey)
        }

    @Test
    fun `마지막 페이지를 받으면 nextKey가 null이어야 한다`() =
        runTest {
            val source = MoviePagingSource(fakePages(totalPages = 2, 1 to listOf(1), 2 to listOf(2)))
            val pager = TestPager(MoviePagingConfig, source)

            pager.refresh()
            val last = pager.append() as PagingSource.LoadResult.Page

            assertNull(last.nextKey)
        }

    @Test
    fun `앞 페이지에서 받은 영화가 다음 페이지에 다시 오면 제외해야 한다`() =
        runTest {
            val source = MoviePagingSource(fakePages(totalPages = 2, 1 to listOf(1, 2), 2 to listOf(2, 3)))
            val pager = TestPager(MoviePagingConfig, source)

            pager.refresh()
            val second = pager.append() as PagingSource.LoadResult.Page

            assertEquals(listOf(3), second.data.map { it.id })
        }

    @Test
    fun `한 페이지의 영화가 전부 중복이면 다음 페이지를 이어서 받아야 한다`() =
        runTest {
            val requested = mutableListOf<Int>()
            val source =
                MoviePagingSource(
                    fakePages(
                        totalPages = 3,
                        1 to listOf(1, 2),
                        2 to listOf(1, 2),
                        3 to listOf(3),
                        requested = requested,
                    ),
                )
            val pager = TestPager(MoviePagingConfig, source)

            pager.refresh()
            val second = pager.append() as PagingSource.LoadResult.Page

            assertEquals(listOf(1, 2, 3), requested)
            assertEquals(listOf(3), second.data.map { it.id })
        }

    @Test
    fun `마지막 페이지의 영화가 전부 중복이면 빈 목록과 nextKey null을 돌려줘야 한다`() =
        runTest {
            val source = MoviePagingSource(fakePages(totalPages = 2, 1 to listOf(1), 2 to listOf(1)))
            val pager = TestPager(MoviePagingConfig, source)

            pager.refresh()
            val last = pager.append() as PagingSource.LoadResult.Page

            assertTrue(last.data.isEmpty())
            assertNull(last.nextKey)
        }

    @Test
    fun `페이지 요청이 실패하면 RemoteError를 담은 RemoteErrorException으로 실패해야 한다`() =
        runTest {
            val source = MoviePagingSource { Outcome.Failure(RemoteError.Network) }

            val result = TestPager(MoviePagingConfig, source).refresh() as PagingSource.LoadResult.Error

            assertEquals(RemoteError.Network, (result.throwable as RemoteErrorException).error)
        }

    @Test
    fun `getRefreshKey는 null이어야 한다`() {
        val source = MoviePagingSource(fakePages(totalPages = 1))

        assertNull(source.getRefreshKey(PagingState(emptyList(), anchorPosition = null, MoviePagingConfig, 0)))
    }
}

private fun fakePages(
    totalPages: Int,
    vararg pages: Pair<Int, List<Int>>,
    requested: MutableList<Int> = mutableListOf(),
): suspend (Int) -> DataResult<MoviePage> {
    val idsByPage = pages.toMap()
    return { page ->
        requested += page
        Outcome.Success(
            MoviePage(
                movies = idsByPage.getValue(page).map(::movie),
                totalPages = totalPages,
            ),
        )
    }
}

private fun movie(id: Int): Movie =
    Movie(
        id = id,
        title = "Movie $id",
        posterUrl = null,
        releaseDate = "",
        overview = "",
        voteAverage = 0.0,
    )
