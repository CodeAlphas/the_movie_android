package com.codealphas.themovie.presentation.movie

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.codealphas.themovie.core.android.theme.TheMovieTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MovieDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TheMovieTheme {
                MovieDetailScreen(onNavigateUp = { onBackPressedDispatcher.onBackPressed() })
            }
        }
    }

    companion object {
        fun createIntent(
            context: Context,
            movieId: Int,
        ): Intent =
            Intent(context, MovieDetailActivity::class.java).apply {
                putExtra(MovieDetailViewModel.ARG_MOVIE_ID, movieId)
            }
    }
}
