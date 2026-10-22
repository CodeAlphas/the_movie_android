package com.codealphas.themovie.presentation.movie.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.codealphas.themovie.domain.movie.Movie
import com.codealphas.themovie.domain.movie.MovieRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow

// 탭 라우트에 인자가 없어 SavedStateHandle로 분류를 받을 수 없으므로, 화면에서 분류를 직접 넘기도록 assisted injection 적용
@HiltViewModel(assistedFactory = MovieListViewModel.Factory::class)
class MovieListViewModel
    @AssistedInject
    constructor(
        repository: MovieRepository,
        @Assisted val category: MovieCategory,
    ) : ViewModel() {
        // 화면 회전이나 탭 전환으로 다시 수집해도 불러온 페이지를 다시 요청하지 않도록, viewModelScope에 보관
        val movies: Flow<PagingData<Movie>> =
            when (category) {
                MovieCategory.POPULAR -> repository.getPopularMovies()
                MovieCategory.TOP_RATED -> repository.getTopRatedMovies()
            }.cachedIn(viewModelScope)

        @AssistedFactory
        interface Factory {
            fun create(category: MovieCategory): MovieListViewModel
        }
    }
