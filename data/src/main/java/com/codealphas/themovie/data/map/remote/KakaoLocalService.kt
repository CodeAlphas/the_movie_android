package com.codealphas.themovie.data.map.remote

import retrofit2.http.GET
import retrofit2.http.Query

internal interface KakaoLocalService {
    // 현재 위치(좌표)의 주소 정보 요청
    @GET("v2/local/geo/coord2address.json")
    suspend fun getAddress(
        @Query("x") longitude: Double,
        @Query("y") latitude: Double,
    ): CoordToAddressDto

    // 현재 위치를 기준으로 주변 영화관 정보 요청
    @GET("v2/local/search/keyword.json")
    suspend fun searchTheaters(
        @Query("x") longitude: Double,
        @Query("y") latitude: Double,
        @Query("query") query: String = "영화관",
        // CT1(문화시설)에는 공연장, 미술관도 있으므로 query와 함께 사용
        @Query("category_group_code") categoryGroupCode: String = "CT1",
        @Query("radius") radiusMeters: Int = SEARCH_RADIUS_METERS,
        @Query("sort") sort: String = "distance",
        @Query("size") size: Int = PAGE_SIZE,
        @Query("page") page: Int = 1,
    ): KeywordSearchDto

    companion object {
        const val SEARCH_RADIUS_METERS = 7_000
        const val PAGE_SIZE = 15
    }
}
