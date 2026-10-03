package com.codealphas.themovie.presentation.movie

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.codealphas.themovie.core.android.theme.Spacing
import com.codealphas.themovie.core.android.ui.rememberThrottledClick
import com.codealphas.themovie.domain.movie.Movie
import com.codealphas.themovie.presentation.R

private val PosterMinWidth = 150.dp
private const val POSTER_ASPECT_RATIO = 2f / 3f
private val CardShape = RoundedCornerShape(10.dp)
private val CardElevation = 4.dp

@Composable
internal fun MovieGrid(
    movies: List<Movie>,
    gridState: LazyGridState,
    onMovieClick: (Int) -> Unit,
) {
    // 2열로 고정하면 가로 화면에서 포스터 사이가 크게 벌어지므로, 넓은 화면에서는 열이 늘도록 최소 폭 적용
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
