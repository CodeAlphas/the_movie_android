package com.codealphas.themovie.presentation.movie

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.codealphas.themovie.core.android.theme.TheMovieTheme
import com.codealphas.themovie.presentation.auth.LoginActivity
import com.codealphas.themovie.presentation.home.HomeScreen
import com.codealphas.themovie.presentation.home.HomeViewModel
import com.codealphas.themovie.presentation.map.MapActivity
import com.codealphas.themovie.presentation.review.ReviewMainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!viewModel.isSignedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        setContent {
            TheMovieTheme {
                HomeScreen(
                    onNavigateToLogin = ::openLogin,
                    onNavigateToDetail = ::openMovieDetail,
                    onNavigateToReview = { startActivity(Intent(this, ReviewMainActivity::class.java)) },
                    onNavigateToMap = { startActivity(Intent(this, MapActivity::class.java)) },
                    viewModel = viewModel,
                )
            }
        }
    }

    private fun openLogin() {
        startActivity(Intent(applicationContext, LoginActivity::class.java))
        finish()
    }

    private fun openMovieDetail(movieId: Int) {
        startActivity(MovieDetailActivity.createIntent(this, movieId))
    }
}
