package com.example.movieapplication.networks

import com.example.movieapplication.models.AddressFromServer
import com.example.movieapplication.models.PoisFromServer
import com.example.movieapplication.utils.Credentials
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface MapRetrofitService {

    // 현재 위치(좌표)의 주소 정보 요청
    @GET("tmap/geo/reversegeocoding")
    suspend fun getCurrentAddress(
        @Header("appKey") appKey: String = Credentials.TMAP_API_KEY,
        @Query("version") version: Int = 1,
        @Query("lat") lat: String, // 위도
        @Query("lon") lon: String, // 경도
        @Query("coordType") coordType: String? = null,
        @Query("addressType") addressType: String? = null,
        @Query("callback") callback: String? = null
    ): AddressFromServer

    // 현재 위치를 기준으로 주변 영화관 정보 요청
    @GET("tmap/pois/search/around")
    suspend fun getTheaterList(
        @Header("appKey") appKey: String = Credentials.TMAP_API_KEY,
        @Query("version") version: Int = 1,
        @Query("centerLon") centerLon: Double, // 반경 검색에서 사용하는 중심 경도
        @Query("centerLat") centerLat: Double, // 반경 검색에서 사용하는 중심 위도
        @Query("categories") categories: String, // 조회 업종 명칭
        @Query("page") page: Int = 1,
        @Query("count") count: Int = 200,
        @Query("radius") radius: Int = 7, // 검색 반경, 1~33km
        @Query("multiPoint") multiPoint: String = "Y",  // Y/N : 멀티입구점 미지원/지원
        @Query("callback") callback: String? = null
    ): PoisFromServer

}