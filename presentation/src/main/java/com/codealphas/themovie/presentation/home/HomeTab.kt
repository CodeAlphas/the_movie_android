package com.codealphas.themovie.presentation.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.codealphas.themovie.presentation.R

enum class HomeTab(
    @param:StringRes val label: Int,
    @param:StringRes val appBarTitle: Int,
    @param:DrawableRes val icon: Int,
) {
    POPULAR(
        label = R.string.movie_list_tab_popular,
        appBarTitle = R.string.movie_list_appbar_popular,
        icon = R.drawable.ic_baseline_movie_popular_24,
    ),
    TOP_RATED(
        label = R.string.movie_list_tab_top_rated,
        appBarTitle = R.string.movie_list_top_rated_subtitle,
        icon = R.drawable.ic_baseline_top_movies_24,
    ),
    SEARCH(
        label = R.string.movie_list_tab_search,
        appBarTitle = R.string.movie_list_appbar_search,
        icon = R.drawable.ic_baseline_search_24,
    ),
}
