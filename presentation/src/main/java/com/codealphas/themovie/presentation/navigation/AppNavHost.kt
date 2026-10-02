package com.codealphas.themovie.presentation.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.codealphas.themovie.presentation.auth.JoinScreen
import com.codealphas.themovie.presentation.auth.LoginScreen
import com.codealphas.themovie.presentation.home.HomeScreen
import com.codealphas.themovie.presentation.map.TheaterMapScreen
import com.codealphas.themovie.presentation.movie.MovieDetailScreen
import com.codealphas.themovie.presentation.review.ReviewEditScreen
import com.codealphas.themovie.presentation.review.ReviewListScreen

@Composable
internal fun AppNavHost(
    startDestination: Any,
    navController: NavHostController = rememberNavController(),
) {
    // 카카오 지도는 SurfaceView에 그려 alpha를 따르지 않아 기본 페이드 중 현재 위치 버튼이 번져 보이므로,
    // 지도 화면만 예외로 두지 않고 모든 화면이 같은 전환을 쓰도록 위치만 바꾸는 가로 슬라이드 적용
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { slideInHorizontally { fullWidth -> fullWidth } },
        exitTransition = { slideOutHorizontally { fullWidth -> -fullWidth } },
        popEnterTransition = { slideInHorizontally { fullWidth -> -fullWidth } },
        popExitTransition = { slideOutHorizontally { fullWidth -> fullWidth } },
    ) {
        navigation<AuthGraph>(startDestination = Login) {
            composable<Login> {
                LoginScreen(
                    onNavigateToHome = navController::navigateToHome,
                    onNavigateToJoin = { navController.navigate(Join) },
                )
            }
            composable<Join> { entry ->
                JoinScreen(onNavigateToLogin = { navController.popBackStackFrom(entry) })
            }
        }
        composable<Home> {
            HomeScreen(
                onNavigateToLogin = navController::navigateToLogin,
                onNavigateToDetail = { movieId -> navController.navigate(MovieDetail(movieId)) },
                onNavigateToReview = { navController.navigate(ReviewList) },
                onNavigateToMap = { navController.navigate(TheaterMap) },
            )
        }
        composable<MovieDetail> { entry ->
            MovieDetailScreen(onNavigateUp = { navController.popBackStackFrom(entry) })
        }
        composable<ReviewList> { entry ->
            ReviewListScreen(
                onNavigateUp = { navController.popBackStackFrom(entry) },
                onNavigateToLogin = navController::navigateToLogin,
                onNavigateToEdit = { reviewId -> navController.navigate(ReviewEdit(reviewId)) },
            )
        }
        composable<ReviewEdit> { entry ->
            ReviewEditScreen(onNavigateUp = { navController.popBackStackFrom(entry) })
        }
        composable<TheaterMap> { entry ->
            TheaterMapScreen(onNavigateUp = { navController.popBackStackFrom(entry) })
        }
    }
}

// 로그인 뒤 뒤로 가면 로그인 화면이 아니라 앱이 닫히도록, 로그인과 회원가입을 back stack에서 제거
private fun NavController.navigateToHome() {
    navigate(Home) {
        popUpTo<AuthGraph> { inclusive = true }
    }
}

// 로그아웃 뒤 뒤로 가면 로그아웃 전 화면이 다시 열리지 않도록, 쌓인 화면을 모두 제거한 뒤 로그인 화면 표시
private fun NavController.navigateToLogin() {
    navigate(AuthGraph) {
        popUpTo(graph.id) { inclusive = true }
    }
}

// 닫히는 전환 중에 뒤로 버튼을 한 번 더 누르면 아래 화면까지 닫히므로, 맨 위 화면일 때만 닫도록 적용
private fun NavController.popBackStackFrom(entry: NavBackStackEntry) {
    if (currentBackStackEntry?.id == entry.id) popBackStack()
}
