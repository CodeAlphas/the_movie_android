package com.example.movieapplication.models

data class PoisFromServer(
    val searchPoiInfo: SearchPoiInfo
)

data class SearchPoiInfo(
    val totalCount: Int,
    val count: Int,
    val page: Int,
    val pois: Pois
)

data class Pois(
    val poi: ArrayList<PoiItem>
)

data class PoiItem(
    val id: String,
    val name: String,
    val telNo: String,
    val frontLat: Double,
    val frontLon: Double,
    val noorLat: Double,
    val noorLon: Double,
    val upperAddrName: String,
    val middleAddrName: String,
    val lowerAddrName: String,
    val detailAddrName: String,
    val mlClass: String,
    val firstNo: String,
    val secondNo: String,
    val roadName: String,
    val radius: String,
    val rpFlag: String,
    val parkFlag: String,
) // TMAP 서버로부터 받은 주변 POI 정보