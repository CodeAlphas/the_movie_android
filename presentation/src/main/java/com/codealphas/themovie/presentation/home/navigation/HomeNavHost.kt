package com.codealphas.themovie.presentation.home.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
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
    onNavigateToDetail: (movieId: Int) -> Unit,
    floatingActionButton: @Composable (visible: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    // 탭 전환은 바깥 NavController의 목적지 변경으로 잡히지 않으므로,
    // 검색 탭에서 키보드가 열린 채 탭을 바꿔도 다른 탭에 키보드가 남지 않도록 탭이 바뀌는 즉시 키보드 닫기 적용
    DisposableEffect(navController, focusManager) {
        val listener = NavController.OnDestinationChangedListener { _, _, _ -> focusManager.clearFocus() }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }
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
                onMovieClick = onNavigateToDetail,
                floatingActionButton = floatingActionButton,
            )
        }
        composable<TopRatedTab> {
            MovieListScreen(
                viewModel = movieListViewModel(MovieCategory.TOP_RATED),
                onMovieClick = onNavigateToDetail,
                floatingActionButton = floatingActionButton,
            )
        }
        composable<SearchTab> {
            SearchMovieScreen(
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
