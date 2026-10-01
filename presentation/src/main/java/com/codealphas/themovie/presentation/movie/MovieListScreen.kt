package com.codealphas.themovie.presentation.movie

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.codealphas.themovie.core.android.theme.Spacing
import com.codealphas.themovie.core.android.theme.TheMovieTheme
import com.codealphas.themovie.core.android.ui.rememberThrottledClick
import com.codealphas.themovie.domain.result.RemoteError
import com.codealphas.themovie.presentation.R
import com.codealphas.themovie.presentation.ui.remoteErrorMessage

private const val PREVIEW_MOVIE_COUNT = 4

@Composable
fun MovieListScreen(
    viewModel: MovieListViewModel,
    snackbarHostState: SnackbarHostState,
    onMovieClick: (Int) -> Unit,
    floatingActionButton: @Composable (visible: Boolean) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 화면이 멈춘 동안 보낸 안내를 돌아와서 받도록, 수집은 STARTED 동안만 하고 채널에 남은 effect는 다시 시작할 때 처리
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is MovieListEffect.ShowError ->
                        snackbarHostState.showSnackbar(
                            message = remoteErrorMessage(resources, effect.error),
                            duration = SnackbarDuration.Long,
                        )
                }
            }
        }
    }

    // 요청이 실패해 목록이 비어 있으면 탭을 다시 고르거나 앱으로 돌아올 때 다시 받도록, 화면이 보일 때마다 요청 적용
    LifecycleResumeEffect(viewModel) {
        viewModel.onIntent(MovieListIntent.PageShown)
        onPauseOrDispose { }
    }

    MovieListContent(
        category = viewModel.category,
        state = state,
        onIntent = viewModel::onIntent,
        onMovieClick = onMovieClick,
        floatingActionButton = floatingActionButton,
    )
}

@Composable
internal fun MovieListContent(
    category: MovieCategory,
    state: MovieListUiState,
    onIntent: (MovieListIntent) -> Unit,
    onMovieClick: (Int) -> Unit,
    floatingActionButton: @Composable (visible: Boolean) -> Unit,
) {
    val gridState = rememberLazyGridState()

    Box(modifier = Modifier.fillMaxSize()) {
        MovieHeaderBackground()
        Column(modifier = Modifier.fillMaxSize()) {
            val (title, subtitle) = category.headerTexts()
            MovieHeaderTexts(title = title, subtitle = subtitle)
            Box(modifier = Modifier.fillMaxSize()) {
                state.movies?.let { movies ->
                    MovieGrid(movies = movies, gridState = gridState, onMovieClick = onMovieClick)
                }
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                val loadError = state.loadError
                if (loadError != null && state.movies == null && !state.isLoading) {
                    LoadErrorContent(
                        error = loadError,
                        onRetry = { onIntent(MovieListIntent.RetryClicked) },
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }
        // XML 화면처럼 목록을 처음 받은 뒤에만 보이고 스크롤하는 동안 숨도록, 목록 유무와 스크롤 상태로 표시 적용
        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(Spacing.medium)) {
            floatingActionButton(state.movies != null && !gridState.isScrollInProgress)
        }
    }
}

private fun MovieCategory.headerTexts(): Pair<Int, Int> =
    when (this) {
        MovieCategory.POPULAR -> R.string.movie_list_popular_title to R.string.movie_list_popular_subtitle
        MovieCategory.TOP_RATED -> R.string.movie_list_top_rated_title to R.string.movie_list_top_rated_subtitle
    }

@Composable
private fun LoadErrorContent(
    error: RemoteError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(Spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Text(
            text = remoteErrorMessage(LocalResources.current, error),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Button(
            onClick = rememberThrottledClick(onClick = onRetry),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
        ) {
            Text(text = stringResource(R.string.common_retry))
        }
    }
}

@Preview(name = "라이트")
@Preview(name = "다크", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MovieListContentPreview() {
    TheMovieTheme {
        Surface {
            MovieListContent(
                category = MovieCategory.POPULAR,
                state = MovieListUiState(movies = List(PREVIEW_MOVIE_COUNT) { previewMovie(it) }),
                onIntent = {},
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
    TheMovieTheme {
        Surface {
            MovieListContent(
                category = MovieCategory.TOP_RATED,
                state = MovieListUiState(loadError = RemoteError.Network),
                onIntent = {},
                onMovieClick = {},
                floatingActionButton = {},
            )
        }
    }
}
