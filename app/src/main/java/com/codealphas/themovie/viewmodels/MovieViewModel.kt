package com.codealphas.themovie.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.DataResult
import com.codealphas.themovie.models.CreditsFromServer
import com.codealphas.themovie.models.MoviesFromServer
import com.codealphas.themovie.models.VideosFromServer
import com.codealphas.themovie.networks.TmdbApiService
import com.codealphas.themovie.networks.safeApiCall
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MovieViewModel
    @Inject
    constructor(
        private val service: TmdbApiService,
    ) : ViewModel() {
        private val _allPopMovies = MutableLiveData<MoviesFromServer>()
        val allPopMovies: MutableLiveData<MoviesFromServer>
            get() = _allPopMovies

        private val _allTopMovies = MutableLiveData<MoviesFromServer>()
        val allTopMovies: MutableLiveData<MoviesFromServer>
            get() = _allTopMovies

        private val _allSearchMovies = MutableLiveData<MoviesFromServer>()
        val allSearchMovies: MutableLiveData<MoviesFromServer>
            get() = _allSearchMovies

        private val _allVideos = MutableLiveData<VideosFromServer>()
        val allVideos: MutableLiveData<VideosFromServer>
            get() = _allVideos

        private val _allCredits = MutableLiveData<CreditsFromServer>()
        val allCredits: MutableLiveData<CreditsFromServer>
            get() = _allCredits

        fun makePopMovieListApiCall() {
            viewModelScope.launch {
                val result = safeApiCall { service.getPopularMovieList() }
                if (result is DataResult.Success) _allPopMovies.value = result.data
            }
        } // TMDB 서버로 인기 영화 정보를 요청하고 해당 정보를 받아오는 메소드

        fun makeTopRatedMovieListApiCall() {
            viewModelScope.launch {
                val result = safeApiCall { service.getTopRatedMovieList() }
                if (result is DataResult.Success) _allTopMovies.value = result.data
            }
        } // TMDB 서버로 높은 평점의 영화 정보를 요청하고 해당 정보를 받아오는 메소드

        fun makeSearchMovieListApiCall(query: String) {
            viewModelScope.launch {
                val result = safeApiCall { service.getSearchedMovieList(query = query) }
                if (result is DataResult.Success) _allSearchMovies.value = result.data
            }
        } // TMDB 서버로 검색한 영화의 정보를 요청하고 해당 정보를 받아오는 메소드

        fun makeVideoApiCall(movieId: Int) {
            viewModelScope.launch {
                val result = safeApiCall { service.getVideosList(movieId = movieId) }
                if (result is DataResult.Success) _allVideos.value = result.data
            }
        } // TMDB 서버로 영화의 동영상 정보를 요청하고 해당 정보를 받아오는 메소드

        fun makeCreditApiCall(movieId: Int) {
            viewModelScope.launch {
                val result = safeApiCall { service.getCreditsList(movieId = movieId) }
                if (result is DataResult.Success) _allCredits.value = result.data
            }
        } // TMDB 서버로 영화 관계자들의 정보를 요청하고 해당 정보를 받아오는 메소드
    }
