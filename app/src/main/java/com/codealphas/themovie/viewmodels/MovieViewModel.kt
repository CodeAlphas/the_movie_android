package com.codealphas.themovie.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.DataResult
import com.codealphas.themovie.domain.RemoteError
import com.codealphas.themovie.models.CreditsFromServer
import com.codealphas.themovie.models.MoviesFromServer
import com.codealphas.themovie.models.VideosFromServer
import com.codealphas.themovie.networks.TmdbApiService
import com.codealphas.themovie.networks.safeApiCall
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
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

        // replay가 1이면 화면이 다시 구독할 때 마지막 오류를 또 받으므로, 같은 안내가 다시 뜨지 않도록 replay를 0으로 설정
        private val _remoteError = MutableSharedFlow<RemoteError>(replay = 0)
        val remoteError: SharedFlow<RemoteError> = _remoteError

        fun makePopMovieListApiCall() {
            fetch({ service.getPopularMovieList() }) { _allPopMovies.value = it }
        } // TMDB 서버로 인기 영화 정보를 요청하고 해당 정보를 받아오는 메소드

        fun makeTopRatedMovieListApiCall() {
            fetch({ service.getTopRatedMovieList() }) { _allTopMovies.value = it }
        } // TMDB 서버로 높은 평점의 영화 정보를 요청하고 해당 정보를 받아오는 메소드

        fun makeSearchMovieListApiCall(query: String) {
            fetch({ service.getSearchedMovieList(query = query) }) { _allSearchMovies.value = it }
        } // TMDB 서버로 검색한 영화의 정보를 요청하고 해당 정보를 받아오는 메소드

        fun makeVideoApiCall(movieId: Int) {
            fetch({ service.getVideosList(movieId = movieId) }) { _allVideos.value = it }
        } // TMDB 서버로 영화의 동영상 정보를 요청하고 해당 정보를 받아오는 메소드

        fun makeCreditApiCall(movieId: Int) {
            fetch({ service.getCreditsList(movieId = movieId) }) { _allCredits.value = it }
        } // TMDB 서버로 영화 관계자들의 정보를 요청하고 해당 정보를 받아오는 메소드

        private fun <T> fetch(
            block: suspend () -> T,
            onSuccess: (T) -> Unit,
        ) {
            viewModelScope.launch {
                when (val result = safeApiCall(block)) {
                    is DataResult.Success -> onSuccess(result.data)
                    is DataResult.Failure -> _remoteError.emit(result.error)
                }
            }
        }
    }
