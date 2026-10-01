package com.codealphas.themovie.presentation

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.codealphas.themovie.presentation.navigation.AppNavHost

@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    AppNavHost(startDestination = viewModel.startDestination)
}
