package com.codealphas.themovie.presentation.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.codealphas.themovie.core.android.theme.TheMovieTheme
import com.codealphas.themovie.presentation.movie.MainActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : ComponentActivity() {
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TheMovieTheme {
                LoginRoute(
                    viewModel = viewModel,
                    onNavigateToMain = ::openMain,
                    onNavigateToJoin = ::openJoin,
                )
            }
        }
    }

    private fun openMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun openJoin() {
        startActivity(Intent(this, JoinActivity::class.java))
    }
}
