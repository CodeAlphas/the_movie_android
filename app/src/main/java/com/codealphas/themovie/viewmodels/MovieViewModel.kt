package com.example.movieapplication.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movieapplication.models.CreditsFromServer
import com.example.movieapplication.models.MoviesFromServer
import com.example.movieapplication.models.VideosFromServer
import com.example.movieapplication.networks.RetroInstance
import com.example.movieapplication.networks.RetrofitService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MovieViewModel : ViewModel() {

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
        viewModelScope.launch(Dispatchers.IO) {
            val retroInstance =
                RetroInstance.getRetrofitInstance().create(RetrofitService::class.java)
            val response = retroInstance.getPopularMovieList()
            _allPopMovies.postValue(response)
        }
    } // TMDB 서버로 인기 영화 정보를 요청하고 해당 정보를 받아오는 메소드

    fun makeTopRatedMovieListApiCall() {
        viewModelScope.launch(Dispatchers.IO) {
            val retroInstance =
                RetroInstance.getRetrofitInstance().create(RetrofitService::class.java)
            val response = retroInstance.getTopRatedMovieList()
            _allTopMovies.postValue(response)
        }
    } // TMDB 서버로 높은 평점의 영화 정보를 요청하고 해당 정보를 받아오는 메소드

    fun makeSearchMovieListApiCall(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val retroInstance =
                RetroInstance.getRetrofitInstance().create(RetrofitService::class.java)
            val response = retroInstance.getSearchedMovieList(query = query)
            _allSearchMovies.postValue(response)
        }
    } // TMDB 서버로 검색한 영화의 정보를 요청하고 해당 정보를 받아오는 메소드

    fun makeVideoApiCall(movieId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val retroInstance =
                RetroInstance.getRetrofitInstance().create(RetrofitService::class.java)
            val response = retroInstance.getVideosList(movieId = movieId)
            _allVideos.postValue(response)
        }
    }  // TMDB 서버로 영화의 동영상 정보를 요청하고 해당 정보를 받아오는 메소드

    fun makeCreditApiCall(movieId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val retroInstance =
                RetroInstance.getRetrofitInstance().create(RetrofitService::class.java)
            val response = retroInstance.getCreditsList(movieId = movieId)
            _allCredits.postValue(response)
        }
    }  // TMDB 서버로 영화 관계자들의 정보를 요청하고 해당 정보를 받아오는 메소드
}