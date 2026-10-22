package com.codealphas.themovie.data.movie

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.codealphas.themovie.domain.movie.Movie
import com.codealphas.themovie.domain.movie.MoviePage
import com.codealphas.themovie.domain.result.DataResult
import com.codealphas.themovie.domain.result.Outcome
import com.codealphas.themovie.domain.result.RemoteErrorException

private const val FIRST_PAGE = 1
private const val PAGE_SIZE = 20

internal val MoviePagingConfig = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false)

internal class MoviePagingSource(
    private val loadPage: suspend (page: Int) -> DataResult<MoviePage>,
) : PagingSource<Int, Movie>() {
    // 순위가 바뀌면 같은 영화가 두 페이지에 나올 수 있고 그러면 그리드 key가 겹치므로, 받은 id를 기억해 제외
    private val loadedIds = mutableSetOf<Int>()

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Movie> = loadFrom(params.key ?: FIRST_PAGE)

    // 한 페이지가 전부 중복이면 화면에 새 항목이 없어 다음 로드를 시작할 계기가 없으므로, 다음 페이지를 이어서 받음
    private tailrec suspend fun loadFrom(page: Int): LoadResult<Int, Movie> =
        when (val result = loadPage(page)) {
            is Outcome.Failure -> LoadResult.Error(RemoteErrorException(result.error))
            is Outcome.Success -> {
                val movies = result.data.movies.filter { loadedIds.add(it.id) }
                val prevKey = (page - 1).takeIf { page > FIRST_PAGE }
                val nextKey = (page + 1).takeIf { page < result.data.totalPages }
                if (movies.isEmpty() && nextKey != null) {
                    loadFrom(nextKey)
                } else {
                    LoadResult.Page(data = movies, prevKey = prevKey, nextKey = nextKey)
                }
            }
        }

    // 목록이 항상 첫 페이지부터 이어지므로, 새로고침도 첫 페이지부터 받도록 null 적용
    override fun getRefreshKey(state: PagingState<Int, Movie>): Int? = null
}
