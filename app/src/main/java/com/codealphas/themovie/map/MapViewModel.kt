package com.codealphas.themovie.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.map.Address
import com.codealphas.themovie.domain.map.Theater
import com.codealphas.themovie.domain.map.TheaterRepository
import com.codealphas.themovie.domain.result.DataResult
import com.codealphas.themovie.domain.result.RemoteError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel
    @Inject
    constructor(
        private val repository: TheaterRepository,
    ) : ViewModel() {
        private val _allTheater = MutableLiveData<List<Theater>>()
        val allTheater: LiveData<List<Theater>>
            get() = _allTheater

        private val _currentAddress = MutableLiveData<Address>()
        val currentAddress: LiveData<Address>
            get() = _currentAddress

        // replay가 1이면 화면이 다시 구독할 때 마지막 오류를 또 받으므로, 같은 안내가 다시 뜨지 않도록 replay를 0으로 설정
        private val _remoteError = MutableSharedFlow<RemoteError>(replay = 0)
        val remoteError: SharedFlow<RemoteError> = _remoteError

        fun makeCurrentAddressApiCall(
            centerLat: String,
            centerLon: String,
        ) {
            fetch({ repository.getAddress(latitude = centerLat, longitude = centerLon) }) {
                _currentAddress.value = it
            }
        }

        fun makeTheaterListApiCall(
            centerLat: Double,
            centerLon: Double,
        ) {
            fetch({ repository.getNearbyTheaters(latitude = centerLat, longitude = centerLon) }) {
                _allTheater.value = it
            }
        }

        private fun <T> fetch(
            block: suspend () -> DataResult<T>,
            onSuccess: (T) -> Unit,
        ) {
            viewModelScope.launch {
                when (val result = block()) {
                    is DataResult.Success -> onSuccess(result.data)
                    is DataResult.Failure -> _remoteError.emit(result.error)
                }
            }
        }
    }
