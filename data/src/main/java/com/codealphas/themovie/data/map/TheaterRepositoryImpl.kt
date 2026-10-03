package com.codealphas.themovie.data.map

import com.codealphas.themovie.data.map.remote.KakaoLocalApiService
import com.codealphas.themovie.data.map.remote.PlaceDto
import com.codealphas.themovie.data.remote.safeApiCall
import com.codealphas.themovie.domain.map.Address
import com.codealphas.themovie.domain.map.Theater
import com.codealphas.themovie.domain.map.TheaterRepository
import com.codealphas.themovie.domain.result.DataResult
import com.codealphas.themovie.domain.result.map
import javax.inject.Inject

internal class TheaterRepositoryImpl
    @Inject
    constructor(
        private val service: KakaoLocalApiService,
    ) : TheaterRepository {
        override suspend fun getAddress(
            latitude: Double,
            longitude: Double,
        ): DataResult<Address> =
            safeApiCall { service.getAddress(longitude = longitude, latitude = latitude) }.map { it.toAddress() }

        override suspend fun getNearbyTheaters(
            latitude: Double,
            longitude: Double,
        ): DataResult<List<Theater>> =
            safeApiCall {
                val places = mutableListOf<PlaceDto>()
                for (page in 1..MAX_PAGES) {
                    val response = service.searchTheaters(longitude = longitude, latitude = latitude, page = page)
                    places += response.documents
                    if (response.meta.isEnd) break
                }
                places.toTheaters()
            }

        private companion object {
            // Kakao Local 키워드 검색은 한 페이지에 최대 15곳이고 지도에 찍을 영화관은 45곳이면 충분하므로, 3페이지까지만 요청
            const val MAX_PAGES = 3
        }
    }
