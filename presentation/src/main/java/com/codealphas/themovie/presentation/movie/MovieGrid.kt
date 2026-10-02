package com.codealphas.themovie.presentation.movie

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.codealphas.themovie.core.android.theme.Spacing
import com.codealphas.themovie.core.android.theme.TheMovieTheme
import com.codealphas.themovie.core.android.ui.rememberThrottledClick
import com.codealphas.themovie.domain.movie.Movie
import com.codealphas.themovie.presentation.R

private val PosterMinWidth = 150.dp
private const val POSTER_ASPECT_RATIO = 2f / 3f
private val CardShape = RoundedCornerShape(10.dp)
private val CardElevation = 4.dp
private val HeaderBackgroundHeight = 210.dp
private val HeaderOvalOverflowX = 90.dp
private val HeaderOvalOverflowTop = 100.dp

@Composable
internal fun MovieHeaderBackground() {
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
internal fun MovieHeaderTexts(
    @StringRes title: Int,
    @StringRes subtitle: Int,
) {
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

@Composable
internal fun MovieGrid(
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

internal fun previewMovie(index: Int): Movie =
    Movie(
        id = index,
        title = "Movie $index",
        posterUrl = null,
        releaseDate = "",
        overview = "",
        voteAverage = 0.0,
    )
