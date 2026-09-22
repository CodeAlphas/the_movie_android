package com.example.movieapplication.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movieapplication.models.AddressFromServer
import com.example.movieapplication.models.PoisFromServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.movieapplication.networks.MapRetrofitService
import com.example.movieapplication.networks.MapRetroInstance

class MapViewModel : ViewModel() {

    private val _allTheater = MutableLiveData<PoisFromServer>()
    val allTheater: LiveData<PoisFromServer>
        get() = _allTheater

    private val _currentAddress = MutableLiveData<AddressFromServer>()
    val currentAddress: LiveData<AddressFromServer>
        get() = _currentAddress

    fun makeCurrentAddressApiCall(centerLat: String, centerLon: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val retroInstance =
                MapRetroInstance.getMapRetrofitInstance()
                    .create(MapRetrofitService::class.java)
            val response = retroInstance.getCurrentAddress(lat = centerLat, lon = centerLon)
            _currentAddress.postValue(response)
        }
    } // TMAP 서버로 현재 위치의 주소 정보를 요청하고 해당 정보를 받아오는 메소드

    fun makeTheaterListApiCall(categories: String, centerLat: Double, centerLon: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val retroInstance =
                MapRetroInstance.getMapRetrofitInstance()
                    .create(MapRetrofitService::class.java)
            val response = retroInstance.getTheaterList(
                categories = categories,
                centerLat = centerLat,
                centerLon = centerLon
            )
            _allTheater.postValue(response)
        }
    } // TMAP 서버로 주변 영화관 정보를 요청하고 해당 정보를 받아오는 메소드
}