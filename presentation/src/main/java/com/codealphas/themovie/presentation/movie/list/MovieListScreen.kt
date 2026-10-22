package com.codealphas.themovie.presentation.movie.list

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.codealphas.themovie.core.android.theme.Spacing
import com.codealphas.themovie.core.android.theme.TheMovieTheme
import com.codealphas.themovie.domain.movie.Movie
import com.codealphas.themovie.domain.result.RemoteError
import com.codealphas.themovie.domain.result.RemoteErrorException
import com.codealphas.themovie.presentation.R
import com.codealphas.themovie.presentation.movie.LoadErrorContent
import com.codealphas.themovie.presentation.movie.MovieFabMenu
import com.codealphas.themovie.presentation.movie.MovieGrid
import com.codealphas.themovie.presentation.movie.previewMovie
import com.codealphas.themovie.presentation.ui.ScreenHeaderBackground
import com.codealphas.themovie.presentation.ui.ScreenHeaderTexts
import com.codealphas.themovie.presentation.ui.remoteError
import kotlinx.coroutines.flow.flowOf

private const val PREVIEW_MOVIE_COUNT = 4

@Composable
fun MovieListScreen(
    viewModel: MovieListViewModel,
    onMovieClick: (Int) -> Unit,
    floatingActionButton: @Composable (visible: Boolean) -> Unit,
) {
    val movies = viewModel.movies.collectAsLazyPagingItems()

    // 첫 로드가 실패해 목록을 받지 못했으면 탭을 다시 고르거나 앱으로 돌아올 때 다시 받도록, 화면이 보일 때마다 재시도 적용
    LifecycleResumeEffect(movies) {
        if (movies.itemCount == 0 && movies.loadState.refresh is LoadState.Error) movies.retry()
        onPauseOrDispose { }
    }

    MovieListContent(
        category = viewModel.category,
        movies = movies,
        onMovieClick = onMovieClick,
        floatingActionButton = floatingActionButton,
    )
}

@Composable
internal fun MovieListContent(
    category: MovieCategory,
    movies: LazyPagingItems<Movie>,
    onMovieClick: (Int) -> Unit,
    floatingActionButton: @Composable (visible: Boolean) -> Unit,
) {
    val gridState = rememberLazyGridState()

    Box(modifier = Modifier.fillMaxSize()) {
        ScreenHeaderBackground()
        Column(modifier = Modifier.fillMaxSize()) {
            val (title, subtitle) = category.headerTexts()
            ScreenHeaderTexts(title = title, subtitle = subtitle)
            Box(modifier = Modifier.fillMaxSize()) {
                if (movies.itemCount > 0) {
                    MovieGrid(movies = movies, gridState = gridState, onMovieClick = onMovieClick)
                }
                when (val refresh = movies.loadState.refresh) {
                    is LoadState.Loading ->
                        if (movies.itemCount == 0) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                    is LoadState.Error ->
                        if (movies.itemCount == 0) {
                            LoadErrorContent(
                                error = refresh.remoteError,
                                onRetry = movies::retry,
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                    is LoadState.NotLoading -> Unit
                }
            }
        }
        // FAB가 영화 목록을 가리지 않도록, 스크롤하는 동안 숨김 적용
        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(Spacing.medium)) {
            floatingActionButton(!gridState.isScrollInProgress)
        }
    }
}

private fun MovieCategory.headerTexts(): Pair<Int, Int> =
    when (this) {
        MovieCategory.POPULAR -> R.string.movie_list_popular_title to R.string.movie_list_popular_subtitle
        MovieCategory.TOP_RATED -> R.string.movie_list_top_rated_title to R.string.movie_list_top_rated_subtitle
    }

@Preview(name = "라이트")
@Preview(name = "다크", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MovieListContentPreview() {
    TheMovieTheme {
        Surface {
            MovieListContent(
                category = MovieCategory.POPULAR,
                movies =
                    flowOf(
                        PagingData.from(List(PREVIEW_MOVIE_COUNT) { previewMovie(it) }),
                    ).collectAsLazyPagingItems(),
                onMovieClick = {},
                floatingActionButton = { visible ->
                    MovieFabMenu(
                        visible = visible,
                        expanded = true,
                        onExpandedChange = {},
                        onReviewClick = {},
                        onMapClick = {},
                    )
                },
            )
        }
    }
}

@Preview(name = "실패")
@Composable
private fun MovieListContentErrorPreview() {
    val notLoading = LoadState.NotLoading(endOfPaginationReached = true)
    val failedStates =
        LoadStates(
            refresh = LoadState.Error(RemoteErrorException(RemoteError.Network)),
            prepend = notLoading,
            append = notLoading,
        )
    TheMovieTheme {
        Surface {
            MovieListContent(
                category = MovieCategory.TOP_RATED,
                movies = flowOf(PagingData.empty<Movie>(sourceLoadStates = failedStates)).collectAsLazyPagingItems(),
                onMovieClick = {},
                floatingActionButton = {},
            )
        }
    }
}
