package com.codealphas.themovie.presentation.map

interface CurrentLocationProvider {
    // 위치를 얻지 못하면 null
    suspend fun getCurrentLocation(): LocationLatLng?
}
