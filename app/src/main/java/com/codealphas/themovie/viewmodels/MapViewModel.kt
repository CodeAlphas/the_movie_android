package com.codealphas.themovie.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.codealphas.themovie.models.AddressFromServer
import com.codealphas.themovie.models.PoisFromServer
import com.codealphas.themovie.networks.MapApiService
import dagger.hilt.android.lifecycle.HiltViewModel
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

        fun makeCurrentAddressApiCall(
            centerLat: String,
            centerLon: String,
        ) {
            launchRequest {
                val response = service.getCurrentAddress(lat = centerLat, lon = centerLon)
                _currentAddress.postValue(response)
            }
        } // TMAP 서버로 현재 위치의 주소 정보를 요청하고 해당 정보를 받아오는 메소드

        fun makeTheaterListApiCall(
            categories: String,
            centerLat: Double,
            centerLon: Double,
        ) {
            launchRequest {
                val response =
                    service.getTheaterList(
                        categories = categories,
                        centerLat = centerLat,
                        centerLon = centerLon,
                    )
                _allTheater.postValue(response)
            }
        } // TMAP 서버로 주변 영화관 정보를 요청하고 해당 정보를 받아오는 메소드
    }
