package com.codealphas.themovie.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.movie.Cast
import com.codealphas.themovie.domain.movie.Movie
import com.codealphas.themovie.domain.movie.MovieRepository
import com.codealphas.themovie.domain.movie.Video
import com.codealphas.themovie.domain.result.DataResult
import com.codealphas.themovie.domain.result.RemoteError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MovieViewModel
    @Inject
    constructor(
        private val repository: MovieRepository,
    ) : ViewModel() {
        private val _allPopMovies = MutableLiveData<List<Movie>>()
        val allPopMovies: LiveData<List<Movie>>
            get() = _allPopMovies

        private val _allTopMovies = MutableLiveData<List<Movie>>()
        val allTopMovies: LiveData<List<Movie>>
            get() = _allTopMovies

        private val _allSearchMovies = MutableLiveData<List<Movie>>()
        val allSearchMovies: LiveData<List<Movie>>
            get() = _allSearchMovies

        private val _allVideos = MutableLiveData<List<Video>>()
        val allVideos: LiveData<List<Video>>
            get() = _allVideos

        private val _allCredits = MutableLiveData<List<Cast>>()
        val allCredits: LiveData<List<Cast>>
            get() = _allCredits

        // replay가 1이면 화면이 다시 구독할 때 마지막 오류를 또 받으므로, 같은 안내가 다시 뜨지 않도록 replay를 0으로 설정
        private val _remoteError = MutableSharedFlow<RemoteError>(replay = 0)
        val remoteError: SharedFlow<RemoteError> = _remoteError

        private var popularLoad: Job? = null
        private var topRatedLoad: Job? = null

        fun makePopMovieListApiCall() {
            popularLoad =
                loadMovies(_allPopMovies.value, popularLoad, { repository.getPopularMovies() }) {
                    _allPopMovies.value = it
                }
        }

        fun makeTopRatedMovieListApiCall() {
            topRatedLoad =
                loadMovies(_allTopMovies.value, topRatedLoad, { repository.getTopRatedMovies() }) {
                    _allTopMovies.value = it
                }
        }

        fun makeSearchMovieListApiCall(query: String) {
            fetch({ repository.searchMovies(query) }) { _allSearchMovies.value = it }
        }

        fun makeVideoApiCall(movieId: Int) {
            fetch({ repository.getVideos(movieId) }) { _allVideos.value = it }
        }

        fun makeCreditApiCall(movieId: Int) {
            fetch({ repository.getCast(movieId) }) { _allCredits.value = it }
        }

        // 응답 전에 다시 호출되면 LiveData가 아직 비어 같은 요청이 한 번 더 나가므로, 진행 중인 요청이 있으면 그 응답을 대기
        private fun loadMovies(
            current: List<Movie>?,
            ongoing: Job?,
            block: suspend () -> DataResult<List<Movie>>,
            onSuccess: (List<Movie>) -> Unit,
        ): Job? {
            if (current != null || ongoing?.isActive == true) return ongoing
            return fetch(block, onSuccess)
        }

        private fun <T> fetch(
            block: suspend () -> DataResult<T>,
            onSuccess: (T) -> Unit,
        ): Job =
            viewModelScope.launch {
                when (val result = block()) {
                    is DataResult.Success -> onSuccess(result.data)
                    is DataResult.Failure -> {
                        // 실패를 빈 목록으로 넣으면 인기와 평점 LiveData가 null이 아니어서,
                        // 탭이 다시 보일 때 재요청이 멈추므로 오류만 전달
                        _remoteError.emit(result.error)
                    }
                }
            }
    }
