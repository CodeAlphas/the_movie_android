package com.codealphas.themovie.presentation.movie

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import coil3.compose.AsyncImage
import com.codealphas.themovie.core.android.theme.Spacing
import com.codealphas.themovie.core.android.theme.TheMovieTheme
import com.codealphas.themovie.core.android.ui.rememberThrottledClick
import com.codealphas.themovie.domain.movie.Movie
import com.codealphas.themovie.domain.result.RemoteError
import com.codealphas.themovie.presentation.R
import com.codealphas.themovie.presentation.ui.remoteErrorMessage

private val PosterMinWidth = 150.dp
private const val POSTER_ASPECT_RATIO = 2f / 3f
private val CardShape = RoundedCornerShape(10.dp)
private val CardElevation = 4.dp
private val HeaderBackgroundHeight = 190.dp
private val HeaderOvalOverflowX = 90.dp
private val HeaderOvalOverflowTop = 100.dp
private const val PREVIEW_MOVIE_COUNT = 4

@Composable
fun MovieListScreen(
    viewModel: MovieListViewModel,
    isCurrentPage: Boolean,
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

    // 실패로 비어 있는 목록을 다시 받도록, 탭이 현재 페이지가 되거나 화면이 다시 RESUMED될 때 요청 적용
    if (isCurrentPage) {
        LifecycleResumeEffect(viewModel) {
            viewModel.onIntent(MovieListIntent.PageShown)
            onPauseOrDispose { }
        }
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
        HeaderBackground()
        Column(modifier = Modifier.fillMaxSize()) {
            MovieListHeader(category = category)
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

@Composable
private fun HeaderBackground() {
    val gradientStart = TheMovieTheme.extendedColors.primaryGradientStart
    val gradientEnd = TheMovieTheme.extendedColors.primaryGradientEnd
    // Canvas는 영역 밖에 그린 부분을 자르지 않아 넘친 타원이 옆 탭 페이지에도 보이므로, 영역 밖을 잘라 내도록 clipToBounds 적용
    Canvas(modifier = Modifier.fillMaxWidth().height(HeaderBackgroundHeight).clipToBounds()) {
        val overflowX = HeaderOvalOverflowX.toPx()
        val overflowTop = HeaderOvalOverflowTop.toPx()
        drawOval(
            brush =
                Brush.verticalGradient(
                    // XML gradient의 angle 90은 시작색을 아래, 끝색을 위에 칠하므로, 위에서 아래로 칠하는 verticalGradient에는 끝색부터 적용
                    colors = listOf(gradientEnd, gradientStart),
                    // XML gradient는 보이는 부분이 아니라 타원 전체에 걸쳐 칠하므로, 같은 색이 나오도록 타원 위끝부터 아래끝까지 범위 적용
                    startY = -overflowTop,
                    endY = size.height,
                ),
            // 목록 위 헤더 배경의 아래 가장자리만 둥글게 보이도록, 타원을 위와 양옆으로 화면 밖까지 크게 배치
            topLeft = Offset(-overflowX, -overflowTop),
            size = Size(size.width + overflowX * 2, size.height + overflowTop),
        )
    }
}

@Composable
private fun MovieListHeader(category: MovieCategory) {
    val (title, subtitle) = category.headerTexts()
    Column(
        modifier = Modifier.padding(start = Spacing.medium, top = Spacing.medium, end = Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(subtitle),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun MovieCategory.headerTexts(): Pair<Int, Int> =
    when (this) {
        MovieCategory.POPULAR -> R.string.movie_list_popular_title to R.string.movie_list_popular_subtitle
        MovieCategory.TOP_RATED -> R.string.movie_list_top_rated_title to R.string.movie_list_top_rated_subtitle
    }

@Composable
private fun MovieGrid(
    movies: List<Movie>,
    gridState: LazyGridState,
    onMovieClick: (Int) -> Unit,
) {
    // XML은 2열 고정이라 가로 화면에서 포스터 사이가 크게 벌어졌으므로, 폰 세로에서는 2열이고 넓은 화면에서는 열이 늘도록 최소 폭 적용
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = PosterMinWidth),
        modifier = Modifier.fillMaxSize(),
        state = gridState,
        contentPadding = PaddingValues(Spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        items(items = movies, key = { it.id }) { movie ->
            MovieCard(movie = movie, onClick = { onMovieClick(movie.id) })
        }
    }
}

@Composable
private fun MovieCard(
    movie: Movie,
    onClick: () -> Unit,
) {
    Card(
        onClick = rememberThrottledClick(onClick = onClick),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation),
    ) {
        val placeholder = painterResource(R.drawable.ic_launcher_foreground)
        AsyncImage(
            model = movie.posterUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().aspectRatio(POSTER_ASPECT_RATIO),
            placeholder = placeholder,
            error = placeholder,
            fallback = placeholder,
            contentScale = ContentScale.Crop,
        )
        Text(
            text = movie.title,
            modifier = Modifier.padding(Spacing.extraSmall),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
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

private fun previewMovie(index: Int): Movie =
    Movie(
        id = index,
        title = "Movie $index",
        posterUrl = null,
        releaseDate = "",
        overview = "",
        voteAverage = 0.0,
    )
