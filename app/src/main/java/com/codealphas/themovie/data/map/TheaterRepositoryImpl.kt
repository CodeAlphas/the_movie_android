package com.codealphas.themovie.data.map

import com.codealphas.themovie.domain.map.Address
import com.codealphas.themovie.domain.map.Theater
import com.codealphas.themovie.domain.map.TheaterRepository
import com.codealphas.themovie.domain.result.DataResult
import com.codealphas.themovie.domain.result.map
import com.codealphas.themovie.networks.MapApiService
import com.codealphas.themovie.networks.safeApiCall
import javax.inject.Inject

class TheaterRepositoryImpl
    @Inject
    constructor(
        private val service: MapApiService,
    ) : TheaterRepository {
        override suspend fun getAddress(
            latitude: String,
            longitude: String,
        ): DataResult<Address> =
            safeApiCall { service.getCurrentAddress(lat = latitude, lon = longitude) }.map { it.toAddress() }

        override suspend fun getNearbyTheaters(
            latitude: Double,
            longitude: Double,
        ): DataResult<List<Theater>> =
            safeApiCall {
                service.getTheaterList(
                    // MapActivity는 영화관만 보여 주므로, 업종을 인자로 받지 않도록 categories에 영화관을 고정
                    categories = "영화관",
                    centerLat = latitude,
                    centerLon = longitude,
                )
            }.map { it.toTheaters() }
    }
