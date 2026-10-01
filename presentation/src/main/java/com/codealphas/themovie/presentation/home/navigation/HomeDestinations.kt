package com.codealphas.themovie.presentation.home.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.codealphas.themovie.presentation.R
import kotlinx.serialization.Serializable

@Serializable
internal data object PopularTab

@Serializable
internal data object TopRatedTab

@Serializable
internal data object SearchTab

internal enum class HomeTab(
    val route: Any,
    @param:StringRes val label: Int,
    @param:StringRes val appBarTitle: Int,
    @param:DrawableRes val icon: Int,
) {
    POPULAR(
        route = PopularTab,
        label = R.string.movie_list_tab_popular,
        appBarTitle = R.string.movie_list_appbar_popular,
        icon = R.drawable.ic_baseline_movie_popular_24,
    ),
    TOP_RATED(
        route = TopRatedTab,
        label = R.string.movie_list_tab_top_rated,
        appBarTitle = R.string.movie_list_top_rated_subtitle,
        icon = R.drawable.ic_baseline_top_movies_24,
    ),
    SEARCH(
        route = SearchTab,
        label = R.string.movie_list_tab_search,
        appBarTitle = R.string.movie_list_appbar_search,
        icon = R.drawable.ic_baseline_search_24,
    ),
}

// 안쪽 NavHost가 첫 화면을 그리기 전에는 목적지가 없으므로, 앱바 제목과 선택 표시가 비지 않도록 시작 탭인 인기 탭 적용
internal fun NavDestination?.toHomeTab(): HomeTab =
    HomeTab.entries.firstOrNull { tab -> this?.hasRoute(tab.route::class) == true } ?: HomeTab.POPULAR
