package com.codealphas.themovie.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codealphas.themovie.domain.DataResult
import com.codealphas.themovie.domain.RemoteError
import com.codealphas.themovie.models.AddressFromServer
import com.codealphas.themovie.models.PoisFromServer
import com.codealphas.themovie.networks.MapApiService
import com.codealphas.themovie.networks.safeApiCall
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel
    @Inject
    constructor(
        private val service: MapApiService,
    ) : ViewModel() {
        private val _allTheater = MutableLiveData<PoisFromServer>()
        val allTheater: LiveData<PoisFromServer>
            get() = _allTheater

        private val _currentAddress = MutableLiveData<AddressFromServer>()
        val currentAddress: LiveData<AddressFromServer>
            get() = _currentAddress

        // replay가 1이면 화면이 다시 구독할 때 마지막 오류를 또 받으므로, 같은 안내가 다시 뜨지 않도록 replay를 0으로 설정
        private val _remoteError = MutableSharedFlow<RemoteError>(replay = 0)
        val remoteError: SharedFlow<RemoteError> = _remoteError

        fun makeCurrentAddressApiCall(
            centerLat: String,
            centerLon: String,
        ) {
            fetch({ service.getCurrentAddress(lat = centerLat, lon = centerLon) }) {
                _currentAddress.value = it
            }
        } // TMAP 서버로 현재 위치의 주소 정보를 요청하고 해당 정보를 받아오는 메소드

        fun makeTheaterListApiCall(
            categories: String,
            centerLat: Double,
            centerLon: Double,
        ) {
            fetch({
                service.getTheaterList(
                    categories = categories,
                    centerLat = centerLat,
                    centerLon = centerLon,
                )
            }) { _allTheater.value = it }
        } // TMAP 서버로 주변 영화관 정보를 요청하고 해당 정보를 받아오는 메소드

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
