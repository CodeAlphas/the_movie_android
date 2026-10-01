package com.codealphas.themovie.presentation.review

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.codealphas.themovie.core.android.theme.TheMovieTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReviewDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TheMovieTheme {
                ReviewEditScreen(onNavigateUp = { onBackPressedDispatcher.onBackPressed() })
            }
        }
    }
}
