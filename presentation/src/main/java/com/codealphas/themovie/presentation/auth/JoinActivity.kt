package com.codealphas.themovie.presentation.auth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.codealphas.themovie.core.android.theme.TheMovieTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class JoinActivity : ComponentActivity() {
    private val viewModel: JoinViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TheMovieTheme {
                JoinRoute(viewModel = viewModel, onNavigateToLogin = ::finish)
            }
        }
    }
}
