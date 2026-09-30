package com.codealphas.themovie.domain.map

import com.codealphas.themovie.domain.result.DataResult

interface TheaterRepository {
    suspend fun getAddress(
        latitude: Double,
        longitude: Double,
    ): DataResult<Address>

    suspend fun getNearbyTheaters(
        latitude: Double,
        longitude: Double,
    ): DataResult<List<Theater>>
}
