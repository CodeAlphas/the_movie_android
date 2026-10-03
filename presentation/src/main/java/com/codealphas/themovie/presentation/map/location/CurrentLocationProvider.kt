package com.codealphas.themovie.presentation.map.location

interface CurrentLocationProvider {
    /**
     * 현재 위치를 한 번 가져온다.
     *
     * @return 현재 위치, 권한이 없거나 위치를 얻지 못하면 null
     */
    suspend fun getCurrentLocation(): LocationLatLng?
}
