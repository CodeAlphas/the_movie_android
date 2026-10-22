package com.codealphas.themovie.presentation.movie

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import com.codealphas.themovie.core.android.theme.Spacing
import com.codealphas.themovie.core.android.ui.rememberThrottledClick
import com.codealphas.themovie.domain.movie.Movie
import com.codealphas.themovie.presentation.R
import com.codealphas.themovie.presentation.ui.remoteError

private val PosterMinWidth = 150.dp
private const val POSTER_ASPECT_RATIO = 2f / 3f
private const val MOVIE_CONTENT_TYPE = "movie"
private val CardShape = RoundedCornerShape(10.dp)
private val CardElevation = 4.dp

@Composable
internal fun MovieGrid(
    movies: List<Movie>,
    gridState: LazyGridState,
    onMovieClick: (Int) -> Unit,
) {
    PosterGrid(gridState = gridState) {
        items(items = movies, key = { it.id }) { movie ->
            MovieCard(movie = movie, onClick = { onMovieClick(movie.id) })
        }
    }
}

@Composable
internal fun MovieGrid(
    movies: LazyPagingItems<Movie>,
    gridState: LazyGridState,
    onMovieClick: (Int) -> Unit,
) {
    PosterGrid(gridState = gridState) {
        items(
            count = movies.itemCount,
            key = movies.itemKey { it.id },
            contentType = movies.itemContentType { MOVIE_CONTENT_TYPE },
        ) { index ->
            movies[index]?.let { movie -> MovieCard(movie = movie, onClick = { onMovieClick(movie.id) }) }
        }
        appendStateItem(movies)
    }
}

@Composable
private fun PosterGrid(
    gridState: LazyGridState,
    content: LazyGridScope.() -> Unit,
) {
    // 2열로 고정하면 가로 화면에서 포스터 사이가 크게 벌어지므로, 넓은 화면에서는 열이 늘도록 최소 폭 적용
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = PosterMinWidth),
        modifier = Modifier.fillMaxSize(),
        state = gridState,
        contentPadding = PaddingValues(Spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        content = content,
    )
}

// 다음 페이지를 받는 동안이나 받지 못했을 때 목록 끝에 아무것도 없으면 스크롤이 멈춘 이유를 알 수 없으므로,
// 마지막 줄 전체에 진행 표시나 다시 시도 버튼 적용
private fun LazyGridScope.appendStateItem(movies: LazyPagingItems<Movie>) {
    when (val append = movies.loadState.append) {
        is LoadState.Loading ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(modifier = Modifier.fillMaxWidth().padding(Spacing.medium), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        is LoadState.Error ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    LoadErrorContent(error = append.remoteError, onRetry = movies::retry)
                }
            }
        is LoadState.NotLoading -> Unit
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
        val placeholder = painterResource(R.drawable.poster_placeholder)
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
