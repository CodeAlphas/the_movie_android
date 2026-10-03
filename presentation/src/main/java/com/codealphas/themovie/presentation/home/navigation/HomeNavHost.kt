package com.codealphas.themovie.presentation.home.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.codealphas.themovie.presentation.movie.list.MovieCategory
import com.codealphas.themovie.presentation.movie.list.MovieListScreen
import com.codealphas.themovie.presentation.movie.list.MovieListViewModel
import com.codealphas.themovie.presentation.movie.search.SearchMovieScreen

@Composable
internal fun HomeNavHost(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    onNavigateToDetail: (movieId: Int) -> Unit,
    floatingActionButton: @Composable (visible: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 하단 탭은 밀어 넘기지 않고 제자리에서 바뀌므로, 탭 사이 페이드가 보이지 않도록 전환 애니메이션 제거
    NavHost(
        navController = navController,
        startDestination = PopularTab,
        modifier = modifier,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
    ) {
        composable<PopularTab> {
            MovieListScreen(
                viewModel = movieListViewModel(MovieCategory.POPULAR),
                snackbarHostState = snackbarHostState,
                onMovieClick = onNavigateToDetail,
                floatingActionButton = floatingActionButton,
            )
        }
        composable<TopRatedTab> {
            MovieListScreen(
                viewModel = movieListViewModel(MovieCategory.TOP_RATED),
                snackbarHostState = snackbarHostState,
                onMovieClick = onNavigateToDetail,
                floatingActionButton = floatingActionButton,
            )
        }
        composable<SearchTab> {
            SearchMovieScreen(
                snackbarHostState = snackbarHostState,
                onMovieClick = onNavigateToDetail,
                floatingActionButton = floatingActionButton,
            )
        }
    }
}

@Composable
private fun movieListViewModel(category: MovieCategory): MovieListViewModel =
    hiltViewModel<MovieListViewModel, MovieListViewModel.Factory> { factory -> factory.create(category) }

// 탭을 오갈 때 back stack이 쌓이지 않고 떠난 탭의 스크롤, 검색어, ViewModel이 남도록 상태 저장과 복원 적용
internal fun NavController.navigateToTab(tab: HomeTab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
