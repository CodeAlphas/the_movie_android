package com.codealphas.themovie.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.codealphas.themovie.models.CreditsFromServer
import com.codealphas.themovie.models.MoviesFromServer
import com.codealphas.themovie.models.VideosFromServer
import com.codealphas.themovie.networks.TmdbApiService
import dagger.hilt.android.lifecycle.HiltViewModel
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
            launchRequest {
                val response = service.getPopularMovieList()
                _allPopMovies.postValue(response)
            }
        } // TMDB 서버로 인기 영화 정보를 요청하고 해당 정보를 받아오는 메소드

        fun makeTopRatedMovieListApiCall() {
            launchRequest {
                val response = service.getTopRatedMovieList()
                _allTopMovies.postValue(response)
            }
        } // TMDB 서버로 높은 평점의 영화 정보를 요청하고 해당 정보를 받아오는 메소드

        fun makeSearchMovieListApiCall(query: String) {
            launchRequest {
                val response = service.getSearchedMovieList(query = query)
                _allSearchMovies.postValue(response)
            }
        } // TMDB 서버로 검색한 영화의 정보를 요청하고 해당 정보를 받아오는 메소드

        fun makeVideoApiCall(movieId: Int) {
            launchRequest {
                val response = service.getVideosList(movieId = movieId)
                _allVideos.postValue(response)
            }
        } // TMDB 서버로 영화의 동영상 정보를 요청하고 해당 정보를 받아오는 메소드

        fun makeCreditApiCall(movieId: Int) {
            launchRequest {
                val response = service.getCreditsList(movieId = movieId)
                _allCredits.postValue(response)
            }
        } // TMDB 서버로 영화 관계자들의 정보를 요청하고 해당 정보를 받아오는 메소드
    }
